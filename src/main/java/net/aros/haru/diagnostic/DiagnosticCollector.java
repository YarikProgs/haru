package net.aros.haru.diagnostic;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticCollector {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void report(Severity severity, String message, SourceSpan span) {
        diagnostics.add(new Diagnostic(severity, message, span));
    }

    public void warn(String message, SourceSpan span) {
        report(Severity.WARNING, message, span);
    }

    public void error(String message, SourceSpan span) {
        report(Severity.ERROR, message, span);
    }

    public void hint(String message, SourceSpan span) {
        report(Severity.HINT, message, span);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR);
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
