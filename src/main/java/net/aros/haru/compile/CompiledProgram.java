package net.aros.haru.compile;

import net.aros.haru.translation.TranslationFile;
import net.aros.haru.value.HaruFunction;

import java.util.Map;

public record CompiledProgram(
        Instruction[] instructions,
        HaruFunction[] functions,
        TranslationFile translationFile,
        Map<String, Integer> slots
) {
}
