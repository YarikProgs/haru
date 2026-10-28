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

    record DoBlock(int target) implements Instruction {
        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) throws DialogueException {
            player.enterBlock(target);
        }
    }

    record Return() implements Instruction {
        public static final Return INSTANCE = new Return();

        @Override
        public void execute(@NotNull AbstractDialoguePlayer player) {
            player.leaveBlock();
        }
    }

    record Negate() implements Instruction {
        public static final Negate INSTANCE = new Negate();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            player.push(player.pop().negate());
        }
    }

    record Invert() implements Instruction {
        public static final Invert INSTANCE = new Invert();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            player.push(player.pop().invert());
        }
    }

    record Add() implements Instruction {
        public static final Add INSTANCE = new Add();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.add(second));
        }
    }

    record Multiply() implements Instruction {
        public static final Multiply INSTANCE = new Multiply();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.mul(second));
        }
    }

    record Divide() implements Instruction {
        public static final Divide INSTANCE = new Divide();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.div(second));
        }
    }

    record Less() implements Instruction {
        public static final Less INSTANCE = new Less();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.less(second));
        }
    }

    record Greater() implements Instruction {
        public static final Greater INSTANCE = new Greater();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.greater(second));
        }
    }

    record Equal() implements Instruction {
        public static final Equal INSTANCE = new Equal();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.equal(second));
        }
    }

    record And() implements Instruction {
        public static final And INSTANCE = new And();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.and(second));
        }
    }

    record Or() implements Instruction {
        public static final Or INSTANCE = new Or();

        @Override
        public void execute(AbstractDialoguePlayer player) {
            Value second = player.pop();
            Value first = player.pop();
            player.push(first.or(second));
        }
    }

    enum Opcode {
        LINE, PARALLEL_LINES, PUSH_CONST, CALL, POP, LOAD, STORE, JUMP, JUMP_IF_FALSE, END,
        DO_BLOCK, RETURN, NEGATE, INVERT, ADD, MULTIPLY, DIVIDE, LESS, GREATER, EQUAL, AND, OR;

        public byte id() {
            return (byte) ordinal();
        }
    }
}
