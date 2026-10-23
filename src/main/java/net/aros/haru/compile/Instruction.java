package net.aros.haru.compile;

import net.aros.haru.runtime.AbstractDialoguePlayer;
import net.aros.haru.runtime.DialogueException;
import net.aros.haru.runtime.Util;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

public sealed interface Instruction {
    void execute(AbstractDialoguePlayer player) throws DialogueException;

    record Line(int argc) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.suspend();
            player.onSingleLine(Util.popRev(player, argc));
        }
    }

    record ParallelLines(int[] argcs) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.suspend();
            player.onMultipleLines(Util.popRev(player, argcs));
        }
    }

    record PushConst(Value constant) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.push(constant);
        }
    }

    record Call(int fn, int argc) implements Instruction {
        @Override
        public void execute(AbstractDialoguePlayer player) throws DialogueException {
            Util.checkedCall(player, fn, Util.popRev(player, argc));
        }
    }

    record Pop() implements Instruction {
        public static final Pop INSTANCE = new Pop();

        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.pop();
        }
    }

    record Load(int slot) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.push(player.get(slot));
        }
    }

    record Store(int slot) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.set(slot, player.pop());
        }
    }

    record Jump(int target) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.jump(target);
        }
    }

    record JumpIfFalse(int target) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            if (player.pop().isFalsey()) player.jump(target);
        }
    }

    record End() implements Instruction {
        public static final End INSTANCE = new End();

        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.terminate();
        }
    }

    enum Opcode {
        LINE, PARALLEL_LINES, PUSH_CONST, CALL, POP, LOAD, STORE, JUMP, JUMP_IF_FALSE, END;

        public byte id() {
            return (byte) ordinal();
        }
    }
}
