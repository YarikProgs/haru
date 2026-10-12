package net.aros.haru.lex;

import org.jetbrains.annotations.Nullable;

public record Token(TokenType type, SourcePos pos, String lexeme, @Nullable Object literal) {
}
