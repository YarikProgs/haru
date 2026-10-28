package net.aros.haru.diagnostic;

import org.jetbrains.annotations.Nullable;

public record Diagnostic(Severity severity, String message, @Nullable SourceSpan span) {
}
