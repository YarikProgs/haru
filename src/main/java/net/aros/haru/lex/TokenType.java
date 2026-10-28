package net.aros.haru.lex;

public enum TokenType {
    // Literals
    NUMBER, STRING, TEXT, IDENTIFIER,

    // Keywords
    NULL, LET, END, TRUE, FALSE, IF, ELIF, ELSE, DO, PARALLEL,
    AND, OR, NOT,

    // Operators
    EQUAL, EQUAL_EQUAL, NOT_EQUAL,
    GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    PLUS, MINUS, STAR, SLASH,

    // Symbols
    COMMA, COLON, SEMICOLON,
    LBRACE, RBRACE, ARROW, LPAREN, RPAREN,

    // Special
    EOF, ERROR
}
