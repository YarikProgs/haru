package net.aros.haru.value;

record TextRefValue(int index) implements Value {
    @Override
    public boolean isTextRef() {
        return true;
    }

    @Override
    public int asTextRef() {
        return index;
    }
}
