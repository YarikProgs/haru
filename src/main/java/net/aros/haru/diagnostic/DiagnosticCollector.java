package net.aros.haru.diagnostic;

import net.aros.haru.lex.SourcePos;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticCollector {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void report(Severity severity, String message, SourcePos pos) {
        diagnostics.add(new Diagnostic(severity, message, pos));
    }

    public void warn(String message, SourcePos pos) {
        report(Severity.WARNING, message, pos);
    }

    public void error(String message, SourcePos pos) {
        report(Severity.ERROR, message, pos);
    }

    public void hint(String message, SourcePos pos) {
        report(Severity.HINT, message, pos);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR);
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
