package net.aros.haru.link;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.value.HaruFunction;

public record LinkedProgram(CompiledProgram program, HaruFunction[] functions) {
}
