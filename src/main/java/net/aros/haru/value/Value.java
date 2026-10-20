package net.aros.haru.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public sealed interface Value permits BoolValue, NullValue, NumberValue, StringValue, TextRefValue {
    default boolean isNumber() {
        return false;
    }

    default boolean isBool() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isTextRef() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default double asNumber() {
        throw new IllegalStateException("Not a number");
    }

    default boolean asBool() {
        throw new IllegalStateException("Not a bool");
    }

    default String asString() {
        throw new IllegalStateException("Not a string");
    }

    default int asTextRef() {
        throw new IllegalStateException("Not a text");
    }

    @Contract("_ -> new")
    static @NotNull Value number(double value) {
        return new NumberValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value bool(boolean value) {
        return new BoolValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value string(String value) {
        return new StringValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value textRef(int index) {
        return new TextRefValue(index);
    }

    static Value nullValue() {
        return NullValue.INSTANCE;
    }

    default boolean isFalsey() {
        if (isBool()) return !asBool();
        if (isNumber()) return asNumber() == 0;
        return isNull();
    }
}
