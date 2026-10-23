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
            "null", NULL,
            "let", LET,
            "true", TRUE,
            "false", FALSE,
            "if", IF,
            "elif", ELIF,
            "else", ELSE
    );
    private static final Set<TokenType> STMT_END_TOKENS = Set.of(
            IDENTIFIER, NUMBER, STRING, TEXT, NULL, COMMA, RPAREN
    );

    private final List<Token> tokens = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final String source;
    private int start, current, line = 1, column = 1;
    private boolean canInsertSemicolon;
    private int parenDepth;

    public Lexer(DiagnosticCollector diagnosticCollector, String source) {
        this.diagnosticCollector = diagnosticCollector;
        this.source = source;
    }

    public List<Token> lexTokens() {
        while (!isAtEnd()) {
            start = current;
            lexToken();
        }
        if (canInsertSemicolon) {
            start = current;
            addToken(SEMICOLON);
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
            case '}' -> addToken(RBRACE);
            case ')' -> {
                addToken(RPAREN);
                if (parenDepth > 0) parenDepth--;
            }
            case '(' -> {
                addToken(LPAREN);
                parenDepth++;
            }
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
                if (canInsertSemicolon && parenDepth == 0) addToken(SEMICOLON);
                newline();
            }
            case '\'' -> string(true);
            case '"' -> string(false);
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
        if ("end".equals(lexeme) && match('.'))
            addToken(END);
        else
            addToken(KEYWORDS.getOrDefault(lexeme, IDENTIFIER));
    }

    private void consumeMultilineComment() {
        while (peek() != '*' && peekNext() != '/') {
            if (isAtEnd()) {
                var pos = new SourcePos(line, column);
                diagnosticCollector.error("Unterminated comment.", pos);
                diagnosticCollector.error("Add */ to terminate the comment", pos);
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

    private void string(boolean literal) {
        char quote = literal ? '\'' : '"';
        while (peek() != quote && peek() != '\n' && !isAtEnd()) {
            advance();
        }
        if (peek() == '\n' || isAtEnd()) {
            var pos = new SourcePos(line, column - current + start);
            diagnosticCollector.error("Unterminated string", pos);
            diagnosticCollector.hint("Add closing '" + quote + "' to terminate the string", pos);
            addString(literal, true);
            return;
        }
        else advance(); // " '
        addString(literal, false);
    }

    private void addString(boolean literal, boolean unterminated) {
        addToken(
                literal ? STRING : TEXT,
                unescapeString(source.substring(start + 1, unterminated ? current : current - 1), column - current + start)
        );
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
                var pos = new SourcePos(line, columnStart + i - 1);
                diagnosticCollector.error("Dangling backslash", pos);
                diagnosticCollector.hint("Use \\\\ for a literal backslash", pos);
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
                case '<' -> sb.append("\\<"); // same
                case '{' -> sb.append("\\{"); // same
                case '0' -> sb.append('\0');
                // TODO: \ uXXXX
                default -> {
                    var pos = new SourcePos(line, columnStart + i - 1);
                    diagnosticCollector.error("Unknown escape '\\" + e + "'", pos);
                    diagnosticCollector.hint("Use \\\\ for a literal backslash", pos);
                }
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
