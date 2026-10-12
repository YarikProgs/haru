package net.aros.haru.value;

public record StringValue(String value) implements Value {
    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String asString() {
        return value;
    }
}
