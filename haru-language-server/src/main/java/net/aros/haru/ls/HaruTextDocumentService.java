package net.aros.haru.ls;

import net.aros.haru.Haru;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.Severity;
import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class HaruTextDocumentService implements TextDocumentService {
    private LanguageClient client;

    public void connect(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(@NotNull DidOpenTextDocumentParams params) {
        TextDocumentItem doc = params.getTextDocument();
        validate(doc.getUri(), doc.getText());
    }

    @Override
    public void didChange(@NotNull DidChangeTextDocumentParams params) {
        String text = params.getContentChanges().getFirst().getText();
        validate(params.getTextDocument().getUri(), text);
    }

    @Override
    public void didClose(@NotNull DidCloseTextDocumentParams params) {
        client.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), List.of()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {

    }

    private void validate(String uri, String text) {
        DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
        Haru.compile(text, diagnosticCollector, UnaryOperator.identity(), null);

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, toLsp4j(diagnosticCollector.diagnostics(), text)));
    }

    private @NotNull List<Diagnostic> toLsp4j(@NotNull List<net.aros.haru.diagnostic.Diagnostic> diagnostics, String source) {
        List<Diagnostic> transformed = new ArrayList<>();
        for (net.aros.haru.diagnostic.Diagnostic diagnostic : diagnostics) {
            var newDiagnostic = toLsp4j(diagnostic, source);
            if (newDiagnostic != null) transformed.add(newDiagnostic);
        }
        return transformed;
    }

    private @Nullable Diagnostic toLsp4j(net.aros.haru.diagnostic.@NotNull Diagnostic diagnostic, String source) {
        Range range = toLsp4j(diagnostic.span());
        if (range == null) return null;
        return new Diagnostic(range, diagnostic.message(), toLsp4j(diagnostic.severity()), source);
    }

    @Contract(pure = true)
    private DiagnosticSeverity toLsp4j(@NotNull Severity severity) {
        return switch (severity) {
            case HINT -> DiagnosticSeverity.Information;
            case WARNING -> DiagnosticSeverity.Warning;
            case ERROR -> DiagnosticSeverity.Error;
        };
    }

    private Range toLsp4j(@Nullable SourceSpan span) {
        if (span == null) return null;
        return new Range(toLsp4j(span.start()), toLsp4j(span.end()));
    }

    @Contract("_ -> new")
    private @NotNull Position toLsp4j(@NotNull SourcePos pos) {
        return new Position(pos.line() - 1, pos.column() - 1);
    }
}
