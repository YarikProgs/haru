package net.aros.haru.lex;

import net.aros.haru.diagnostic.SourceSpan;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Token(TokenType type, SourceSpan span, String lexeme, @Nullable Object literal) {
    @Contract("_ -> new")
    public static @NotNull Token syntheticText(String source) {
        return new Token(TokenType.TEXT, SourceSpan.selection(1, 1, source.length()), source, source);
    }
}
