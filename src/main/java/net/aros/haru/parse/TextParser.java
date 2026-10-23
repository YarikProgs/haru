package net.aros.haru.parse;

import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Parses double-quoted string into parts
 */
public class TextParser {
    private final DiagnosticCollector diagnosticCollector;
    private final String source;
    private final List<TextPart> parts = new ArrayList<>();
    private final StringBuilder literal = new StringBuilder();
    private final Deque<OpenTag> stack = new ArrayDeque<>();
    private int start, current, line, column, startLine, startColumn;

    public TextParser(DiagnosticCollector diagnosticCollector, @NotNull Token source) {
        this.diagnosticCollector = diagnosticCollector;
        if (source.type() != TokenType.TEXT) {
            throw new IllegalArgumentException("Expected text (double-quoted string), got: '" + source.lexeme() + "'");
        }
        this.source = (String) source.literal();
        this.line = source.pos().line();
        this.column = source.pos().column();
    }

    public @NotNull ParsedText parse() {
        if (source.isEmpty())
            return new ParsedText(source, new TextPart.Literal(""));

        while (!isAtEnd()) {
            mark();
            parsePart();
        }

        closeUnclosed();
        flushLiteral(parts, literal);
        return new ParsedText(source, new TextPart.Parts(parts));
    }

    private void parsePart() {
        char c = advance();
        if (c == '\\' && (peek() == '<' || peek() == '{')) {
            currentLiteral().append(advance()); // < | {
        } else if (c == '{') {
            placeholder();
        } else if (c == '<') {
            tag();
        } else {
            currentLiteral().append(c);
        }
    }

    private void placeholder() {
        start = current;
        skipUntil('}');
        if (isAtEnd()) {
            recoverUnterminatedPlaceholder();
            return;
        }
        advance(); // }
        String raw = source.substring(start, current - 1);
        String name = raw.trim();
        if (!isValidName(name)) {
            recoverInvalidPlaceholder(raw, name);
            return;
        }
        flushLiteral();
        currentParts().add(new TextPart.Placeholder(name));
    }

    private void recoverInvalidPlaceholder(String raw, String name) {
        if (name.isEmpty()) {
            diagnosticCollector.error("Empty placeholder '{}'", startPos());
        } else {
            diagnosticCollector.error("Invalid placeholder name '{" + raw + "}'", startPos());
        }
        currentLiteral().append('{').append(raw).append('}');
    }

    private void recoverUnterminatedPlaceholder() {
        String raw = source.substring(start, current);
        String name = raw.trim();
        if (!isValidName(name)) {
            diagnosticCollector.error("Unterminated placeholder", startPos());
            currentLiteral().append('{').append(raw);
            return;
        }
        diagnosticCollector.error("Unterminated placeholder '{" + name + "', expected '}'", startPos());
        flushLiteral();
        currentParts().add(new TextPart.Placeholder(name));
    }

    private void tag() {
        if (match('/')) {
            closingTag();
            return;
        }
        if (isAtEnd() || !isAlpha(peek())) {
            diagnosticCollector.error("Invalid tag syntax, expected tag name after '<'", startPos());
            currentLiteral().append('<');
            return;
        }
        String name = scanName();
        List<String> arguments = new ArrayList<>();
        switch (tagEnd(name, arguments)) {
            case OPEN -> {
                flushLiteral();
                stack.push(new OpenTag(name, arguments, startLine, startColumn));
            }
            case SELF_CLOSE, UNTERMINATED -> {
                flushLiteral();
                currentParts().add(new TextPart.Tag(name, List.copyOf(arguments)));
            }
        }
    }

    private TagEnd tagEnd(String name, List<String> arguments) {
        while (true) {
            skipWhitespace();
            if (isAtEnd()) {
                diagnosticCollector.error("Unterminated tag '<" + name + ">', expected '>' or '/>'", startPos());
                return TagEnd.UNTERMINATED;
            }
            TagEnd end = tagEndChar(name, arguments);
            if (end != null) return end;
        }
    }

    private TagEnd tagEndChar(String name, List<String> arguments) {
        char c = peek();
        if (c == '>') {
            advance();
            return TagEnd.OPEN;
        }
        if (c == '/') return slashInTag(name);
        if (isArgChar(c)) {
            scanArgument(arguments);
            return null;
        }
        diagnosticCollector.error("Invalid character '" + c + "' in tag '<" + name + ">'", currentPos());
        advance();
        return null;
    }

    private TagEnd slashInTag(String name) {
        advance(); // /
        skipWhitespace();
        if (match('>')) return TagEnd.SELF_CLOSE;
        diagnosticCollector.error("Expected '>' after '/' in tag '<" + name + ">'", currentPos());
        return null;
    }

    private void closingTag() {
        skipWhitespace();
        if (isAtEnd() || !isAlpha(peek())) {
            recoverInvalidClose();
            return;
        }
        String name = scanName();
        skipWhitespace();
        if (!match('>')) {
            recoverUnterminatedClose(name);
        }
        processClose(name);
    }

    private void recoverInvalidClose() {
        diagnosticCollector.error("Invalid closing tag, expected tag name after '</'", startPos());
        currentLiteral().append("</");
    }

    private void recoverUnterminatedClose(String name) {
        if (isAtEnd()) {
            diagnosticCollector.error("Unterminated closing tag '</" + name + ">', expected '>'", startPos());
            return;
        }
        diagnosticCollector.error("Invalid closing tag '</" + name + ">', expected '>'", startPos());
        skipUntil('>');
        if (!isAtEnd()) advance(); // >
    }

    private void processClose(String name) {
        if (stack.isEmpty()) {
            diagnosticCollector.error("Unexpected closing tag '</" + name + ">'", startPos());
            return;
        }
        OpenTag top = stack.peek();
        if (!top.tag().equals(name)) {
            reportMismatch(name, top.tag());
            return;
        }
        stack.pop();
        flushLiteral(top.inner(), top.literal());
        currentParts().add(closeFrame(top));
    }

    private void reportMismatch(String found, String expected) {
        diagnosticCollector.error("Mismatched closing tag '</" + found + ">', expected '</" + expected + ">'", startPos());
    }

    private void closeUnclosed() {
        while (!stack.isEmpty()) {
            closeUnclosedTag();
        }
    }

    private void closeUnclosedTag() {
        OpenTag frame = stack.pop();
        flushLiteral(frame.inner(), frame.literal());
        diagnosticCollector.error("Unclosed tag '<" + frame.tag() + ">', expected '</" + frame.tag() + ">'", frame.pos());
        if (frame.inner().isEmpty()) {
            currentParts().add(emptyBiTag(frame));
        } else {
            currentParts().add(closeFrame(frame));
        }
    }

    private TextPart closeFrame(OpenTag frame) {
        if (frame.inner().isEmpty()) {
            diagnosticCollector.warn("Empty paired tag '<" + frame.tag() + "></" + frame.tag() + ">'", frame.pos());
            return emptyBiTag(frame);
        }
        TextPart inner = frame.inner().size() == 1
                ? frame.inner().getFirst()
                : new TextPart.Parts(List.copyOf(frame.inner()));
        return new TextPart.BiTag(frame.tag(), List.copyOf(frame.arguments()), inner);
    }

    private TextPart emptyBiTag(OpenTag frame) {
        return new TextPart.BiTag(frame.tag(), List.copyOf(frame.arguments()), new TextPart.Literal(""));
    }

    private List<TextPart> currentParts() {
        return stack.isEmpty() ? parts : stack.peek().inner();
    }

    private StringBuilder currentLiteral() {
        return stack.isEmpty() ? literal : stack.peek().literal();
    }

    private void flushLiteral() {
        flushLiteral(currentParts(), currentLiteral());
    }

    private void flushLiteral(List<TextPart> target, StringBuilder buffer) {
        if (buffer.isEmpty()) return;
        target.add(new TextPart.Literal(buffer.toString()));
        buffer.setLength(0);
    }

    private String scanName() {
        start = current;
        do advance();
        while (isTagNamePart(peek()));
        return source.substring(start, current);
    }

    private void scanArgument(List<String> arguments) {
        start = current;
        while (isArgChar(peek())) advance();
        arguments.add(source.substring(start, current));
    }

    private void skipWhitespace() {
        while (!isAtEnd() && isWhitespace(peek())) advance();
    }

    private void skipUntil(char target) {
        while (!isAtEnd() && peek() != target) advance();
    }

    private void mark() {
        start = current;
        startLine = line;
        startColumn = column;
    }

    private SourcePos startPos() {
        return new SourcePos(startLine, startColumn);
    }

    private SourcePos currentPos() {
        return new SourcePos(line, column);
    }

    private boolean isValidName(String name) {
        if (name.isEmpty() || !isAlpha(name.charAt(0))) return false;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!isAlpha(c) && !isDigit(c)) return false;
        }
        return true;
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
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private boolean isTagNamePart(char c) {
        return isAlpha(c) || isDigit(c) || c == '-';
    }

    private boolean isArgChar(char c) {
        return !isWhitespace(c) && c != '<' && c != '>' && c != '{' && c != '}'
                && c != '/' && c != '\\' && c != '"' && c != '\'' && c != '\0';
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

    private enum TagEnd {
        OPEN,
        SELF_CLOSE,
        UNTERMINATED
    }

    private record OpenTag(String tag, List<String> arguments, List<TextPart> inner, StringBuilder literal, int line,
                           int column) {
        OpenTag(String tag, List<String> arguments, int line, int column) {
            this(tag, arguments, new ArrayList<>(), new StringBuilder(), line, column);
        }

        SourcePos pos() {
            return new SourcePos(line, column);
        }
    }
}
