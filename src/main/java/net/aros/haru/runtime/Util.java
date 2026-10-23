package net.aros.haru.runtime;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.diagnostic.Severity;
import net.aros.haru.value.HaruFunction;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Util {
    public static void checkedCall(@NotNull AbstractDialoguePlayer player, int fn, Value[] args) throws DialogueException {
        HaruFunction function = player.program().functions()[fn];

        Value val = Value.nullValue();
        try {
            val = function.call(player, args);
        } catch (Exception e) {
            Util.error(RuntimeDiagnostic.ErrorCode.FUNCTION_EXCEPTION, "Unexpected exception", e);
        }
        player.push(val);
    }

    public static void checkSlots(@NotNull CompiledProgram program, @NotNull Map<String, Value> externalVariables) throws DialogueException {
        Set<String> extra = new HashSet<>(externalVariables.keySet());
        extra.removeAll(program.slots().keySet());
        if (extra.isEmpty()) return;

        error(RuntimeDiagnostic.ErrorCode.EXTRA_EXTERNAL_VARIABLES, "Extra external variables: " + extra);
    }

    public static void mergeSlots(CompiledProgram program, Value[] slots, @NotNull Map<String, Value> externalVariables) {
        externalVariables.forEach((variable, value) -> slots[program.slots().get(variable)] = value);
    }

    public static void error(RuntimeDiagnostic.ErrorCode code, String message, @Nullable Throwable cause) throws DialogueException {
        throw new DialogueException(new RuntimeDiagnostic(Severity.ERROR, code, message, cause));
    }

    public static void error(RuntimeDiagnostic.ErrorCode code, String message) throws DialogueException {
        error(code, message, null);
    }

    public static Value @NotNull [] popRev(AbstractDialoguePlayer player, int argc) {
        Value[] argv = new Value[argc];
        for (int i = 0; i < argc; i++) {
            argv[argc - i - 1] = player.pop();
        }
        return argv;
    }

    public static Value @NotNull [][] popRev(AbstractDialoguePlayer player, int @NotNull [] argcs) {
        Value[][] args = new Value[argcs.length][];
        for (int i = 0; i < argcs.length; i++) {
            args[argcs.length - i - 1] = popRev(player, argcs[i]);
        }
        return args;
    }
}
