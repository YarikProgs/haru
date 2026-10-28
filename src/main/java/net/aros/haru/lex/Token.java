package net.aros.haru.lex;

import net.aros.haru.diagnostic.SourceSpan;
import org.jetbrains.annotations.Nullable;

public record Token(TokenType type, SourceSpan span, String lexeme, @Nullable Object literal) {
}
