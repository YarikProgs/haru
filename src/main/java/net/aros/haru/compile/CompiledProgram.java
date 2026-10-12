package net.aros.haru.compile;

import net.aros.haru.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CompiledProgram {
    private final Map<String, Value> defaults;
    private final Map<String, Value> values;
    private final List<Instruction> instructions;

    public CompiledProgram(Map<String, Value> defaults, Map<String, Value> values, List<Instruction> instructions) {
        this.defaults = Collections.unmodifiableMap(defaults);
        this.values = Collections.unmodifiableMap(values);
        this.instructions = Collections.unmodifiableList(instructions);
    }

    public Optional<Value> get(String name) {
        Value value = values.get(name);
        if (value != null) return Optional.of(value);
        return Optional.ofNullable(defaults.get(name));
    }

    public List<Instruction> instructions() {
        return instructions;
    }

    public Map<String, Value> values() {
        return values;
    }
}
