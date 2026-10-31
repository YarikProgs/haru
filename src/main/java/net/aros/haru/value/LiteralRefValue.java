package net.aros.haru.value;

record LiteralRefValue(int index) implements Value {
    @Override
    public Value equal(Value other) {
        return other.isLiteralRef() && other.asLiteralRef() == index ? TRUE : FALSE;
    }

    @Override
    public String toString() {
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
