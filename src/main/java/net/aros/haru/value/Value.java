package net.aros.haru.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public sealed interface Value permits NullValue, NumberValue, LiteralRefValue, TextRefValue {
    Value TRUE = number(1);
    Value FALSE = number(0);

    default Value add(Value other) {
        throw new IllegalStateException("Cannot ADD between " + this + " and " + other.toString());
    }

    default Value mul(Value other) {
        throw new IllegalStateException("Cannot MUL between " + this + " and " + other.toString());
    }

    default Value div(Value other) {
        throw new IllegalStateException("Cannot DIV between " + this + " and " + other.toString());
    }

    default Value greater(Value other) {
        throw new IllegalStateException("Cannot GREATER between " + this + " and " + other.toString());
    }

    default Value less(Value other) {
        throw new IllegalStateException("Cannot LESS between " + this + " and " + other.toString());
    }

    default Value and(Value second) {
        return isFalsey() || second.isFalsey() ? FALSE : TRUE;
    }

    default Value or(Value second) {
        return isFalsey() && isFalsey() ? FALSE : TRUE;
    }

    Value equal(Value other);

    default Value negate() {
        throw new IllegalStateException("Cannot NEGATE " + this);
    }

    default Value invert() {
        return isFalsey() ? TRUE : FALSE;
    }

    String toString();

    default boolean isNumber() {
        return false;
    }

    default boolean isLiteralRef() {
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

    default int asTextRef() {
        throw new IllegalStateException("Not a text");
    }

    default int asLiteralRef() {
        throw new IllegalStateException("Not a literal");
    }

    @Contract("_ -> new")
    static @NotNull Value number(double value) {
        return new NumberValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value number(boolean value) {
        return number(value ? 1 : 0);
    }

    @Contract("_ -> new")
    static @NotNull Value literalRef(int index) {
        return new LiteralRefValue(index);
    }

    @Contract("_ -> new")
    static @NotNull Value textRef(int index) {
        return new TextRefValue(index);
    }

    static Value nullValue() {
        return NullValue.INSTANCE;
    }

    default boolean isFalsey() {
        if (isNumber()) return asNumber() == 0;
        return isNull();
    }

    enum Tag {
        NUMBER, LITERAL_REF, TEXT_REF, NULL;

        public byte id() {
            return (byte) ordinal();
        }
    }
}
