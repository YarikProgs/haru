package net.aros.haru.runtime;

import net.aros.haru.diagnostic.Severity;
import org.jetbrains.annotations.Nullable;

public record RuntimeDiagnostic(
        Severity severity,
        ErrorCode code,
        String message,
        @Nullable Throwable cause
) {
    public enum ErrorCode {
        EXTRA_EXTERNAL_VARIABLES,
        CALL_DEPTH_EXCEEDED, FUNCTION_EXCEPTION
    }
}
