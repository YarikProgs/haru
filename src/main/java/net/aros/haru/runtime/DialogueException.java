package net.aros.haru.runtime;

import org.jetbrains.annotations.Nullable;

public class DialogueException extends Exception {
    public DialogueException(String message, @Nullable Throwable cause) {
        super(message, cause, false, false);
    }

    public DialogueException(String message) {
        this(message, null);
    }
}
