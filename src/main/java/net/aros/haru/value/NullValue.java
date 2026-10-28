package net.aros.haru.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

final class NullValue implements Value {
    static final Value INSTANCE = new NullValue();

    private NullValue() {}

    @Contract("_ -> new")
    @Override
    public @NotNull Value equal(Value other) {
        return Value.number(this == INSTANCE);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String asString() {
        return "null";
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public String toString() {
        return "NullValue";
    }
}
