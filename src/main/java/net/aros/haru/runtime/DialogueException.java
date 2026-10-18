package net.aros.haru.runtime;

public class DialogueException extends Exception {
    private final RuntimeDiagnostic diagnostic;

    public DialogueException(RuntimeDiagnostic diagnostic) {
        super(null, null, false, false);
        this.diagnostic = diagnostic;
    }

    public RuntimeDiagnostic diagnostic() {
        return diagnostic;
    }
}
