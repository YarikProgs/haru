package net.aros.haru.ls;

import net.aros.haru.Haru;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.Severity;
import net.aros.haru.lex.SourcePos;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
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
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem doc = params.getTextDocument();
        validate(doc.getUri(), doc.getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String text = params.getContentChanges().getFirst().getText();
        validate(params.getTextDocument().getUri(), text);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
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

    private List<Diagnostic> toLsp4j(List<net.aros.haru.diagnostic.Diagnostic> diagnostics, String source) {
        List<Diagnostic> transformed = new ArrayList<>();
        for (net.aros.haru.diagnostic.Diagnostic diagnostic : diagnostics) {
            var newDiagnostic = toLsp4j(diagnostic, source);
            if (newDiagnostic != null) transformed.add(newDiagnostic);
        }
        return transformed;
    }

    private Diagnostic toLsp4j(net.aros.haru.diagnostic.Diagnostic diagnostic, String source) {
        Position position = toLsp4j(diagnostic.pos());
        if (position == null) return null;
        return new Diagnostic(new Range(position, position), diagnostic.message(), toLsp4j(diagnostic.severity()), source);
    }

    private DiagnosticSeverity toLsp4j(Severity severity) {
        return switch (severity) {
            case HINT -> DiagnosticSeverity.Hint;
            case WARNING -> DiagnosticSeverity.Warning;
            case ERROR -> DiagnosticSeverity.Error;
        };
    }

    private Position toLsp4j(@Nullable SourcePos pos) {
        return pos == null ? null : new Position(pos.line() - 1, pos.column() - 1);
    }
}
