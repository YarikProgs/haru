package net.aros.haru.lex;

import net.aros.haru.diagnostic.DiagnosticCollector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.aros.haru.lex.TokenType.*;

public class Lexer {
    private static final Map<String, TokenType> KEYWORDS = Map.of(
            "null", NULL
    );
    private static final Set<TokenType> STMT_END_TOKENS = Set.of(
            IDENTIFIER, NUMBER, STRING, NULL
    );

    private final List<Token> tokens = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final String source;
    private int start, current, line = 1, column = 1;
    private boolean canInsertSemicolon = false;

    public Lexer(DiagnosticCollector diagnosticCollector, String source) {
        this.diagnosticCollector = diagnosticCollector;
        this.source = source;
    }

    public List<Token> lexTokens() {
        while (!isAtEnd()) {
            start = current;
            lexToken();
        }
        tokens.add(new Token(EOF, new SourcePos(line, column), "", null));
        return tokens;
    }

    private void lexToken() {
        char c = advance();

        if (isDigit(c)) {
            number();
            return;
        }
        if (isAlpha(c)) {
            identifierOrKeyword();
            return;
        }

        switch (c) {
            case '=' -> addToken(EQUAL);
            case ',' -> addToken(COMMA);
            case ';' -> addToken(SEMICOLON);
            case ':' -> addToken(COLON);
            case '{' -> addToken(LBRACE);
            case '#' -> consumeComment();
            case '-' -> {
                if (match('>'))
                    addToken(ARROW);
                else
                    unknownToken();
            }
            case '/' -> {
                if (match('/'))
                    consumeComment();
                else if (match('*'))
                    consumeMultilineComment();
                else
                    unknownToken();
            }
            case '\n' -> {
                if (canInsertSemicolon) addToken(SEMICOLON);
                newline();
            }
            case '\'', '"' -> string();
            case ' ', '\r', '\t' -> {
            }
            default -> unknownToken();
        }
    }

    private void unknownToken() {
        diagnosticCollector.error("Unknown token: " + source.substring(start, current), new SourcePos(line, column - current + start));
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            do advance();
            while (isDigit(peek()));
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifierOrKeyword() {
        while (isAlpha(peek()) || isDigit(peek())) advance();
        String lexeme = source.substring(start, current);
        addToken(KEYWORDS.getOrDefault(lexeme, IDENTIFIER));
    }

    private void consumeMultilineComment() {
        while (peek() != '*' && peekNext() != '/') {
            if (isAtEnd()) {
                diagnosticCollector.error("Unterminated comment", new SourcePos(line, column));
                return;
            }
            if (advance() == '\n') newline();
        }
        advance(); // *
        advance(); // /
    }

    private void newline() {
        line++;
        column = 1;
    }

    private void consumeComment() {
        while (peek() != '\n' && !isAtEnd()) advance();
    }

    private void string() {
        while (peek() != '"' && peek() != '\'' && !isAtEnd()) {
            advance();
        }
        if (isAtEnd())
            diagnosticCollector.error("Unterminated string", new SourcePos(line, column - current + start));
        else
            advance(); // " '
        addToken(STRING, unescapeString(source.substring(start + 1, current - 1), column - current + start));
    }

    private String unescapeString(String string, int columnStart) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }

            if (++i >= string.length()) {
                diagnosticCollector.error("Dangling backslash", new SourcePos(line, columnStart + i - 1));
                break;
            }
            char e = string.charAt(i);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case '\\' -> sb.append('\\');
                case '"' -> sb.append('"');
                case '\'' -> sb.append('\'');
                case '0' -> sb.append('\0');
                // TODO: \ uXXXX
                default ->
                        diagnosticCollector.error("Unknown escape '\\" + e + "'", new SourcePos(line, columnStart + i - 1));
            }
        }
        return sb.toString();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, @Nullable Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, new SourcePos(line, column - current + start), text, literal));
        canInsertSemicolon = STMT_END_TOKENS.contains(type);
    }

    private boolean match(char c) {
        if (peek() == c) {
            advance();
            return true;
        }
        return false;
    }

    private char advance() {
        if (isAtEnd()) return '\0';
        column++;
        return source.charAt(current++);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}
