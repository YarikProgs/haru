package net.aros.haru.ls;

import net.aros.haru.Haru;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.Severity;
import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.IdentifierCollector;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public class HaruTextDocumentService implements TextDocumentService {
    private final Map<String, IdentifierCollector> identifierCollectors = new ConcurrentHashMap<>();
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
        identifierCollectors.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {

    }

    private void validate(String uri, String text) {
        DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
        IdentifierCollector identifierCollector = new IdentifierCollector();
        Haru.compile(
                text,
                diagnosticCollector,
                identifierCollector,
                UnaryOperator.identity(),
                null
        );

        identifierCollectors.put(uri, identifierCollector);
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, toLsp4j(diagnosticCollector.diagnostics())));
    }

    @Override
    public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
            PrepareRenameParams params) {
        int line = params.getPosition().getLine() + 1;
        int column = params.getPosition().getCharacter() + 1;
        return CompletableFuture.completedFuture(
                findOccurrenceInAt(params.getTextDocument().getUri(), line, column)
                        .map(oc -> Either3.<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>forFirst(toLsp4j(oc.at())))
                        .orElse(null)
        );
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        int line = params.getPosition().getLine() + 1;
        int column = params.getPosition().getCharacter() + 1;
        return CompletableFuture.completedFuture(
                findOccurrenceInAt(params.getTextDocument().getUri(), line, column)
                        .map(oc -> oc.all().stream().map(span -> new TextEdit(toLsp4j(span), params.getNewName())).toList())
                        .map(edits -> new WorkspaceEdit(Map.of(params.getTextDocument().getUri(), edits)))
                        .orElse(null)
        );
    }

    private Optional<IdentifierCollector.Occurrence> findOccurrenceInAt(String uri, int line, int column) {
        var identifierCollector = identifierCollectors.get(uri);
        if (identifierCollector == null)
            return Optional.empty();

        return Optional.ofNullable(identifierCollector.occurrenceAt(line, column));
    }

    private @NotNull List<Diagnostic> toLsp4j(@NotNull List<net.aros.haru.diagnostic.Diagnostic> diagnostics) {
        List<Diagnostic> transformed = new ArrayList<>();
        for (net.aros.haru.diagnostic.Diagnostic diagnostic : diagnostics) {
            var newDiagnostic = toLsp4j(diagnostic);
            if (newDiagnostic != null) transformed.add(newDiagnostic);
        }
        return transformed;
    }

    private @Nullable Diagnostic toLsp4j(net.aros.haru.diagnostic.@NotNull Diagnostic diagnostic) {
        Range range = toLsp4j(diagnostic.span());
        if (range == null) return null;
        return new Diagnostic(range, diagnostic.message(), toLsp4j(diagnostic.severity()), "haru");
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
