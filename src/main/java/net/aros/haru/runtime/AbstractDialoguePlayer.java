package net.aros.haru.runtime;

import net.aros.haru.ast.TextPart;
import net.aros.haru.compile.Instruction;
import net.aros.haru.link.LinkedProgram;
import net.aros.haru.runtime.RuntimeDiagnostic.ErrorCode;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractDialoguePlayer {
    private static final int MAX_CALL_DEPTH = 1024;
    private final LinkedProgram linkedProgram;
    private final Deque<Value> stack = new ArrayDeque<>();
    private final Deque<Integer> callStack = new ArrayDeque<>();
    private final Value[] slots;
    private final AbstractTextRenderer.Factory textRendererFactory;
    private final FuzzyPolicy fuzzyPolicy;
    private int ip;
    private boolean done, suspended;

    public AbstractDialoguePlayer(
            @NotNull LinkedProgram linkedProgram,
            @NotNull Map<String, Value> externalVariables,
            @NotNull AbstractTextRenderer.Factory textRendererFactory,
            @NotNull FuzzyPolicy fuzzyPolicy
    ) throws DialogueException {
        this.linkedProgram = Objects.requireNonNull(linkedProgram);
        this.slots = new Value[linkedProgram.program().slots().size()];
        this.textRendererFactory = Objects.requireNonNull(textRendererFactory);
        this.fuzzyPolicy = Objects.requireNonNull(fuzzyPolicy);
        Arrays.fill(slots, Value.nullValue());
        Util.checkSlots(linkedProgram.program(), externalVariables);
        Util.mergeSlots(linkedProgram.program(), slots, externalVariables);
    }

    public final void update() throws DialogueException {
        if (done || suspended) return;

        Instruction instruction = linkedProgram.program().instructions()[ip++];
        instruction.execute(this);
    }

    public abstract void onSingleLine(Value[] args) throws DialogueException;

    public abstract void onMultipleLines(Value[][] args) throws DialogueException;

    public final String renderText(TextPart part, String locale) throws DialogueException {
        var textRenderer = textRendererFactory.create(this, locale);
        textRenderer.visit(part);
        return textRenderer.toString();
    }

    public final String renderValue(@NotNull Value value, String locale) throws DialogueException {
        if (value.isLiteralRef())
            return linkedProgram.program().literals()[value.asLiteralRef()];
        else if (value.isTextRef())
            return renderText(Util.getEntry(linkedProgram.program(), value.asTextRef(), locale, fuzzyPolicy).text(), locale);
        else
            return value.toString();
    }

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

    public final void enterBlock(int target) throws DialogueException {
        if (callStack.size() >= MAX_CALL_DEPTH)
            Util.error(ErrorCode.CALL_DEPTH_EXCEEDED, "Block call depth exceeded (recursion?)");
        callStack.push(ip);
        ip = target;
    }

    public final void leaveBlock() {
        ip = callStack.pop();
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

    /* getters */
    public final @NotNull LinkedProgram linkedProgram() {
        return linkedProgram;
    }

    public final boolean isDone() {
        return done;
    }

    public final boolean isSuspended() {
        return suspended;
    }
}
