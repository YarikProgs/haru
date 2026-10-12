package net.aros.haru.value;

record NumberValue(double value) implements Value {
    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public double asNumber() {
        return value;
    }
}