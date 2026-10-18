package net.aros.haru.value;

import net.aros.haru.ast.TextPart;
import net.aros.haru.parse.ParsedText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public sealed interface Value permits BoolValue, NullValue, NumberValue, StringValue, TextValue {
    default boolean isNumber() {
        return false;
    }

    default boolean isBool() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isText() {
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

    default TextPart asText() {
        throw new IllegalStateException("Not a text");
    }

    default String asRawText() {
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
    static @NotNull Value text(ParsedText parsedText) {
        return new TextValue(parsedText);
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
