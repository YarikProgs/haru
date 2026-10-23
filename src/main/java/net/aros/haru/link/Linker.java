package net.aros.haru.link;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.value.HaruFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class Linker {
    private final DiagnosticCollector diagnosticCollector;
    private final CompiledProgram program;
    private final HaruFunction[] functions;

    public Linker(DiagnosticCollector diagnosticCollector, @NotNull CompiledProgram program) {
        this.diagnosticCollector = diagnosticCollector;
        this.program = program;
        this.functions = new HaruFunction[program.functionSlots().size()];
    }

    public Linker define(String name, @NotNull HaruFunction function) {
        Integer slot = program.functionSlots().get(name);
        if (slot == null) return this; // the function isn't used by the program

        functions[slot] = Objects.requireNonNull(function);
        return this;
    }

    public Linker defineAll(@NotNull Map<String, HaruFunction> functions) {
        functions.forEach(this::define);
        return this;
    }

    public LinkedProgram link() {
        findMissing();
        if (diagnosticCollector.hasErrors()) {
            return null;
        }
        return new LinkedProgram(program, functions);
    }

    private void findMissing() {
        String[] names = new String[program.functionSlots().size()];
        program.functionSlots().forEach((key, value) -> names[value] = key);

        for (int i = 0; i < functions.length; i++) {
            if (functions[i] != null) continue;

            diagnosticCollector.error("Unknown function: " + names[i], null);
        }
    }
}
