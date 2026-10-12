package net.aros.haru.value;

final class NullValue implements Value {
    static final Value INSTANCE = new NullValue();

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public String toString() {
        return "NullValue";
    }
}
