package net.aros.haru.runtime;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Instruction;
import net.aros.haru.link.LinkedProgram;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;

public abstract class AbstractDialoguePlayer {
    private final LinkedProgram program;
    private final Deque<Value> stack = new ArrayDeque<>();
    private final Value[] slots;
    private int ip;
    private boolean done, suspended;

    public AbstractDialoguePlayer(@NotNull LinkedProgram program, Map<String, Value> externalVariables) throws DialogueException {
        this.program = program;
        this.slots = new Value[program.program().slots().size()];
        Arrays.fill(slots, Value.nullValue());
        Util.checkSlots(program.program(), externalVariables);
        Util.mergeSlots(program.program(), slots, externalVariables);
    }

    public final void update() throws DialogueException {
        if (done || suspended) return;

        Instruction instruction = program.program().instructions()[ip++];
        instruction.execute(this);
    }

    public abstract void onLine(Value[] args);

    public abstract void onLines(Value[][] args);

    /* stack */
    public final Value pop() {
        return stack.pop();
    }

    public final void push(Value value) {
        stack.push(value);
    }

    /* slots */
    public final Value get(int slot) {
        return slots[slot];
    }

    public final void set(int slot, Value value) {
        slots[slot] = value;
    }

    /* flow */
    public final void jump(int target) {
        ip = target;
    }

    public final void suspend() {
        suspended = true;
    }

    public final void resume() {
        suspended = false;
    }

    public final void terminate() {
        done = true;
    }

    /* properties */
    public final @NotNull LinkedProgram program() {
        return program;
    }

    public final boolean isDone() {
        return done;
    }

    public final boolean isSuspended() {
        return suspended;
    }
}
