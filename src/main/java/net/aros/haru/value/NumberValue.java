package net.aros.haru.value;

record NumberValue(double value) implements Value {
    @Override
    public Value add(Value other) {
        if (other.isNumber()) return Value.number(value + other.asNumber());
        return Value.super.add(other);
    }

    @Override
    public Value mul(Value other) {
        if (other.isNumber()) return Value.number(value * other.asNumber());
        return Value.super.mul(other);
    }

    @Override
    public Value div(Value other) {
        if (other.isNumber()) return Value.number(value / other.asNumber());
        return Value.super.div(other);
    }

    @Override
    public Value greater(Value other) {
        if (other.isNumber()) return Value.number(value > other.asNumber());
        return Value.super.greater(other);
    }

    @Override
    public Value less(Value other) {
        if (other.isNumber()) return Value.number(value < other.asNumber());
        return Value.super.less(other);
    }

    @Override
    public Value equal(Value other) {
        if (other.isNumber()) return Value.number(value == other.asNumber());
        return Value.FALSE;
    }

    @Override
    public Value negate() {
        return Value.number(-value);
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public double asNumber() {
        return value;
    }

    @Override
    public String asString() {
        String value = String.valueOf(this.value);
        if (value.endsWith(".0")) return value.substring(0, value.length() - 2);
        return value;
    }
}