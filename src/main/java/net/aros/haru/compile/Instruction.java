package net.aros.haru.compile;

import net.aros.haru.value.Value;

import java.util.List;

public sealed interface Instruction {
    record Command(String command, List<Value> arguments) implements Instruction {
    }

    record Line(List<Value> values) implements Instruction {
    }

    record ParallelLines(List<Line> lines) implements Instruction {
    }
}
