package net.aros.haru.value;

import org.jetbrains.annotations.NotNull;

record LiteralRefValue(int index) implements Value {
    @Override
    public Value equal(Value other) {
        return other.isLiteralRef() && other.asLiteralRef() == index ? TRUE : FALSE;
    }

    @Override
    public @NotNull String toString() {
        return "<string literal #" + index + ">";
    }

    @Override
    public boolean isLiteralRef() {
        return true;
    }

    @Override
    public int asLiteralRef() {
        return index;
    }
}
