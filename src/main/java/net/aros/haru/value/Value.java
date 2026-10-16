package net.aros.haru.value;

import net.aros.haru.ast.TextPart;
import net.aros.haru.parse.ParsedText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public sealed interface Value permits NullValue, NumberValue, PrefixedValue, StringValue, TextValue, VariableValue {
    default boolean isNumber() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isText() {
        return false;
    }

    default boolean isPrefixedValue() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default double asNumber() {
        throw new IllegalStateException("Not a number");
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

    default PrefixedValue asPrefixedValue() {
        throw new IllegalStateException("Not a prefixed value");
    }

    @Contract("_ -> new")
    static @NotNull Value number(double value) {
        return new NumberValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value string(String value) {
        return new StringValue(value);
    }

    @Contract("_ -> new")
    static @NotNull Value text(ParsedText parsedText) {
        return new TextValue(parsedText);
    }

    @Contract("_, _ -> new")
    static @NotNull Value prefixedValue(String prefix, Value value) {
        return new PrefixedValue(prefix, value);
    }

    @Contract(value = "_ -> new", pure = true)
    static @NotNull Value variableValue(Supplier<Value> supplier) {
        return new VariableValue(supplier);
    }

    static Value nullValue() {
        return NullValue.INSTANCE;
    }
}
