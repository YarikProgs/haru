package net.aros.haru.value;

record TextRefValue(int index) implements Value {
    @Override
    public Value equal(Value other) {
        return other.isTextRef() && other.asTextRef() == index ? TRUE : FALSE;
    }

    @Override
    public String toString() {
        return "<text #" + index + ">";
    }

    @Override
    public boolean isTextRef() {
        return true;
    }

    @Override
    public int asTextRef() {
        return index;
    }
}
