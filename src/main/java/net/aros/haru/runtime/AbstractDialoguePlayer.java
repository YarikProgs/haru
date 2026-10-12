package net.aros.haru.runtime;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Instruction;

public abstract class AbstractDialoguePlayer {
    private final CompiledProgram program;
    protected int ip = 0;

    public AbstractDialoguePlayer(CompiledProgram program) {
        this.program = program;
    }

    public final void update() {
        if (isDone()) return;

        Instruction instruction = program.instructions().get(ip++);
        switch (instruction) {
            case Instruction.Line line -> onLine(line);
            case Instruction.ParallelLines lines -> onParallelLines(lines);
            case Instruction.Command command -> onCommand(command);
        }
    }

    public final boolean isDone() {
        return ip < 0 || ip >= program.instructions().size();
    }

    protected abstract void onLine(Instruction.Line line);

    protected abstract void onParallelLines(Instruction.ParallelLines lines);

    protected abstract void onCommand(Instruction.Command command);
}
