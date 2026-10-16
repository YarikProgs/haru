package net.aros.haru.lex;

public enum TokenType {
    // Literals
    NUMBER, STRING, TEXT, IDENTIFIER,

    // Keywords
    NULL,

    // Symbols
    EQUAL, COMMA, COLON, SEMICOLON,
    LBRACE, ARROW, LPAREN, RPAREN,

    // Special
    EOF, ERROR
}
