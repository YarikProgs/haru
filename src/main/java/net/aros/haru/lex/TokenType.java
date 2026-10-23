package net.aros.haru.lex;

public enum TokenType {
    // Literals
    NUMBER, STRING, TEXT, IDENTIFIER,

    // Keywords
    NULL, LET, END, TRUE, FALSE, IF, ELIF, ELSE,

    // Symbols
    EQUAL, COMMA, COLON, SEMICOLON,
    LBRACE, RBRACE, ARROW, LPAREN, RPAREN,

    // Special
    EOF, ERROR
}
