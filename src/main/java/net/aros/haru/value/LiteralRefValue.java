package net.aros.haru.value;

record LiteralRefValue(int index) implements Value {
    @Override
    public boolean isLiteralRef() {
        return true;
    }

    @Override
    public int asLiteralRef() {
        return index;
    }
}
