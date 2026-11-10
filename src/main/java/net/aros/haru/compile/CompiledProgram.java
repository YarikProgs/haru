package net.aros.haru.compile;

import net.aros.haru.translation.TranslationFile;
import net.aros.haru.value.Value;

import java.util.Map;

public record CompiledProgram(
        Instruction[] instructions,
        TranslationFile translationFile,
        String[] literals,
        Map<String, Value> exports,
        Map<String, Integer> slots,
        Map<String, Integer> functionSlots
) {
}
