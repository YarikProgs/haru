package net.aros.haru.lex;

import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
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
            "else", ELSE,
            "do", DO,
            "parallel", PARALLEL
    );
    private static final Set<TokenType> STMT_END_TOKENS = Set.of(
            IDENTIFIER, NUMBER, STRING, TEXT, NULL, COMMA, RPAREN
    );

    private final List<Token> tokens = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final IdentifierCollector identifierCollector;
    private final String source;
    private int start, current, line = 1, column = 1;
    private boolean canInsertSemicolon;
    private int parenDepth;

    public Lexer(DiagnosticCollector diagnosticCollector, @Nullable IdentifierCollector identifierCollector, String source) {
        this.diagnosticCollector = diagnosticCollector;
        this.identifierCollector = identifierCollector;
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
        tokens.add(new Token(EOF, SourceSpan.dot(line, column), "", null));
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
            }
            case '\'' -> string(true);
            case '"' -> string(false);
            case ' ', '\r', '\t' -> {
            }
            default -> unknownToken();
        }
    }

    private void unknownToken() {
        int len = current - start;
        diagnosticCollector.error("Unknown token: " + source.substring(start, current), SourceSpan.selection(line, column - len, len));
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
        SourcePos start = new SourcePos(line, column - 2);
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        var span = new SourceSpan(start, new SourcePos(line, column));
        diagnosticCollector.error("Unterminated comment.", span);
        diagnosticCollector.hint("Add */ to terminate the comment", SourceSpan.dot(line, column + 1));
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
            int len = current - start;
            var span = SourceSpan.selection(line, column - len, len);
            diagnosticCollector.error("Unterminated string", span);
            diagnosticCollector.hint("Add closing '" + quote + "' to terminate the string", SourceSpan.dot(line, column + 1));
            addString(literal, true);
            return;
        } else advance(); // " '
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
                var span = SourceSpan.dot(line, columnStart + i - 1);
                diagnosticCollector.error("Dangling backslash", span);
                diagnosticCollector.hint("Use \\\\ for a literal backslash", span);
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
                    var pos = SourceSpan.dot(line, columnStart + i - 1);
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
        int len = current - start;
        SourceSpan span = SourceSpan.selection(line, column - len, len);
        tokens.add(new Token(
                type,
                span,
                text,
                literal
        ));
        canInsertSemicolon = STMT_END_TOKENS.contains(type);

        if (identifierCollector != null && type == IDENTIFIER)
            identifierCollector.add(text, span);
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
        char c = source.charAt(current++);
        if (c == '\n') newline();
        else column++;
        return c;
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
