package net.aros.haru.diagnostic;

import net.aros.haru.lex.SourcePos;

public record Diagnostic(Severity severity, String message, SourcePos pos) {
}
