package net.aros.haru.diagnostic;

import net.aros.haru.lex.SourcePos;
import org.jetbrains.annotations.Nullable;

public record Diagnostic(Severity severity, String message, @Nullable SourcePos pos) {
}
