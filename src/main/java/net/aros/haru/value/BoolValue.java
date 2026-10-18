package net.aros.haru.value;

record BoolValue(boolean value) implements Value {
    @Override
    public boolean isBool() {
        return true;
    }

    @Override
    public boolean asBool() {
        return value;
    }
}
