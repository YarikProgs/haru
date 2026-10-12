package net.aros.haru.value;

public record PrefixedValue(String prefix, Value value) implements Value {
    @Override
    public boolean isPrefixedValue() {
        return true;
    }

    @Override
    public PrefixedValue asPrefixedValue() {
        return this;
    }
}