package net.aros.haru.compile;

import java.util.Map;

public record CompiledProgram(Instruction[] instructions, Map<String, Integer> slots) {
}
