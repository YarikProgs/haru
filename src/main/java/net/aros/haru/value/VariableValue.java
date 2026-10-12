package net.aros.haru.value;

import java.util.function.Supplier;

public final class VariableValue implements Value {
    private final Supplier<Value> supplier;

    VariableValue(Supplier<Value> supplier) {
        this.supplier = supplier;
    }

    @Override
    public boolean isNull() {
        return supplier.get().isNull();
    }

    @Override
    public boolean isNumber() {
        return supplier.get().isNumber();
    }

    @Override
    public boolean isString() {
        return supplier.get().isString();
    }

    @Override
    public boolean isPrefixedValue() {
        return supplier.get().isPrefixedValue();
    }

    @Override
    public double asNumber() {
        return supplier.get().asNumber();
    }

    @Override
    public String asString() {
        return supplier.get().asString();
    }

    @Override
    public PrefixedValue asPrefixedValue() {
        return supplier.get().asPrefixedValue();
    }

    @Override
    public String toString() {
        return "VariableValue[value=" + supplier.get() + "]";
    }
}
