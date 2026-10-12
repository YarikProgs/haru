package net.aros.haru.lex;

public enum TokenType {
    // Literals
    NUMBER, STRING, IDENTIFIER,

    // Keywords
    NULL,

    // Symbols
    EQUAL, COMMA, COLON, SEMICOLON,
    LBRACE, ARROW,

    // Special
    EOF, ERROR
}
