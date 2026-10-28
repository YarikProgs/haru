package net.aros.haru.parse;

import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.IdentifierCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Parses double-quoted string into parts
 */
public class TextParser {
    private final DiagnosticCollector diagnostics;
    private final @Nullable IdentifierCollector identifiers;
    private final String text;
    private final int line;
    private final int firstColumn;
    private final Scope root = new Scope();
    private final Deque<OpenTag> openTags = new ArrayDeque<>();
    private int partStart;
    private int position;

    public TextParser(DiagnosticCollector diagnosticCollector,
                      @Nullable IdentifierCollector identifierCollector,
                      @NotNull Token token) {
        if (token.type() != TokenType.TEXT) {
            throw new IllegalArgumentException("Expected text (double-quoted string), got: '" + token.lexeme() + "'");
        }
        this.diagnostics = diagnosticCollector;
        this.identifiers = identifierCollector;
        this.text = (String) token.literal();
        this.line = token.span().start().line();
        this.firstColumn = token.span().start().column() + 1;
    }

    public @NotNull ParsedText parse() {
        if (text.isEmpty()) {
            return new ParsedText(text, new TextPart.Literal(""));
        }
        while (!isAtEnd()) {
            parseNextPart();
        }
        closeUnclosedTags();
        return new ParsedText(text, new TextPart.Parts(root.takeParts()));
    }

    private void parseNextPart() {
        partStart = position;
        char c = advance();
        if (c == '\\' && isEscapable(peek())) {
            currentScope().append(advance());
        } else if (c == '{') {
            parsePlaceholder();
        } else if (c == '<') {
            parseTag();
        } else {
            currentScope().append(c);
        }
    }

    private void parsePlaceholder() {
        int contentStart = position;
        skipUntil('}');
        boolean terminated = !isAtEnd();
        String raw = text.substring(contentStart, position);
        match('}');

        String name = raw.trim();
        if (isValidName(name)) {
            acceptPlaceholder(name, contentStart + countLeadingBlanks(raw), terminated);
        } else {
            rejectPlaceholder(raw, name, terminated);
        }
    }

    private void acceptPlaceholder(String name, int nameStart, boolean terminated) {
        if (!terminated) {
            diagnostics.error("Unterminated placeholder '{" + name + "', expected '}'", spanFromPartStart());
        }
        if (identifiers != null) {
            identifiers.add(name, spanOf(nameStart, nameStart + name.length()));
        }
        currentScope().add(new TextPart.Placeholder(name));
    }

    private void rejectPlaceholder(String raw, String name, boolean terminated) {
        diagnostics.error(describeInvalidPlaceholder(raw, name, terminated), spanFromPartStart());
        Scope scope = currentScope();
        scope.append('{');
        scope.append(raw);
        if (terminated) scope.append('}');
    }

    private String describeInvalidPlaceholder(String raw, String name, boolean terminated) {
        if (!terminated) return "Unterminated placeholder";
        if (name.isEmpty()) return "Empty placeholder '{}'";
        return "Invalid placeholder name '{" + raw + "}'";
    }

    private void parseTag() {
        if (match('/')) {
            parseClosingTag();
            return;
        }
        if (!isAlpha(peek())) {
            diagnostics.error("Invalid tag syntax, expected tag name after '<'", spanFromPartStart());
            currentScope().append('<');
            return;
        }
        String name = scanName();
        List<String> arguments = new ArrayList<>();
        TagEnd end = scanTagEnd(name, arguments);
        if (end == TagEnd.OPEN) {
            openTag(name, arguments);
        } else {
            currentScope().add(new TextPart.Tag(name, List.copyOf(arguments)));
        }
    }

    private void openTag(String name, List<String> arguments) {
        currentScope().flushLiteral();
        openTags.push(new OpenTag(name, arguments, spanFromPartStart(), new Scope()));
    }

    private TagEnd scanTagEnd(String name, List<String> arguments) {
        while (true) {
            skipWhitespace();
            if (isAtEnd()) {
                diagnostics.error("Unterminated tag '<" + name + ">', expected '>' or '/>'", spanFromPartStart());
                return TagEnd.UNTERMINATED;
            }
            char c = peek();
            if (c == '>') {
                advance();
                return TagEnd.OPEN;
            }
            if (c == '/') {
                if (scanSelfClose(name)) return TagEnd.SELF_CLOSE;
            } else if (isArgChar(c)) {
                arguments.add(scanArgument());
            } else {
                skipInvalidTagChar(name);
            }
        }
    }

    private boolean scanSelfClose(String name) {
        advance();
        skipWhitespace();
        if (match('>')) return true;
        diagnostics.error("Expected '>' after '/' in tag '<" + name + ">'", spanOfCurrentChar());
        return false;
    }

    private void skipInvalidTagChar(String name) {
        diagnostics.error("Invalid character '" + peek() + "' in tag '<" + name + ">'", spanOfCurrentChar());
        advance();
    }

    private void parseClosingTag() {
        skipWhitespace();
        if (!isAlpha(peek())) {
            diagnostics.error("Invalid closing tag, expected tag name after '</'", spanFromPartStart());
            currentScope().append("</");
            return;
        }
        String name = scanName();
        skipWhitespace();
        if (!match('>')) {
            reportBrokenClosingTag(name);
        }
        closeTag(name);
    }

    private void reportBrokenClosingTag(String name) {
        if (isAtEnd()) {
            diagnostics.error("Unterminated closing tag '</" + name + ">', expected '>'", spanFromPartStart());
            return;
        }
        skipUntil('>');
        match('>');
        diagnostics.error("Invalid closing tag '</" + name + ">', expected '>'", spanFromPartStart());
    }

    private void closeTag(String name) {
        if (openTags.isEmpty()) {
            diagnostics.error("Unexpected closing tag '</" + name + ">'", spanFromPartStart());
            return;
        }
        OpenTag open = openTags.peek();
        if (!open.name().equals(name)) {
            diagnostics.error("Mismatched closing tag '</" + name + ">', expected '</" + open.name() + ">'",
                    spanFromPartStart());
            return;
        }
        openTags.pop();
        List<TextPart> inner = open.scope().takeParts();
        if (inner.isEmpty()) {
            diagnostics.warn("Empty paired tag '<" + name + "></" + name + ">'",
                    new SourceSpan(open.span().start(), posAt(position)));
        }
        currentScope().add(toBiTag(open, inner));
    }

    private void closeUnclosedTags() {
        while (!openTags.isEmpty()) {
            OpenTag open = openTags.pop();
            diagnostics.error("Unclosed tag '<" + open.name() + ">', expected '</" + open.name() + ">'", open.span());
            currentScope().add(toBiTag(open, open.scope().takeParts()));
        }
    }

    private TextPart toBiTag(OpenTag open, List<TextPart> inner) {
        return new TextPart.BiTag(open.name(), List.copyOf(open.arguments()), toSinglePart(inner));
    }

    private TextPart toSinglePart(List<TextPart> parts) {
        if (parts.isEmpty()) return new TextPart.Literal("");
        if (parts.size() == 1) return parts.getFirst();
        return new TextPart.Parts(List.copyOf(parts));
    }

    private Scope currentScope() {
        return openTags.isEmpty() ? root : openTags.peek().scope();
    }

    private String scanName() {
        int start = position;
        advance();
        while (isTagNamePart(peek())) advance();
        return text.substring(start, position);
    }

    private String scanArgument() {
        int start = position;
        while (isArgChar(peek())) advance();
        return text.substring(start, position);
    }

    private void skipWhitespace() {
        while (isWhitespace(peek())) advance();
    }

    private void skipUntil(char target) {
        while (!isAtEnd() && peek() != target) advance();
    }

    private int countLeadingBlanks(String raw) {
        int count = 0;
        while (count < raw.length() && raw.charAt(count) <= ' ') count++;
        return count;
    }

    private SourcePos posAt(int index) {
        return new SourcePos(line, firstColumn + index);
    }

    private SourceSpan spanOf(int from, int to) {
        return new SourceSpan(posAt(from), posAt(to));
    }

    private SourceSpan spanFromPartStart() {
        return spanOf(partStart, position);
    }

    private SourceSpan spanOfCurrentChar() {
        return spanOf(position, Math.min(position + 1, text.length()));
    }

    private boolean match(char expected) {
        if (isAtEnd() || peek() != expected) return false;
        advance();
        return true;
    }

    private char advance() {
        return isAtEnd() ? '\0' : text.charAt(position++);
    }

    private char peek() {
        return isAtEnd() ? '\0' : text.charAt(position);
    }

    private boolean isAtEnd() {
        return position >= text.length();
    }

    private static boolean isEscapable(char c) {
        return c == '<' || c == '{';
    }

    private static boolean isValidName(String name) {
        return !name.isEmpty()
                && isAlpha(name.charAt(0))
                && name.chars().skip(1).allMatch(c -> isAlpha((char) c) || isDigit((char) c));
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static boolean isTagNamePart(char c) {
        return isAlpha(c) || isDigit(c) || c == '-';
    }

    private static boolean isArgChar(char c) {
        return !isWhitespace(c) && "<>{}/\\\"'\0".indexOf(c) < 0;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private enum TagEnd {
        OPEN,
        SELF_CLOSE,
        UNTERMINATED
    }

    private record OpenTag(String name, List<String> arguments, SourceSpan span, Scope scope) {
    }

    private static final class Scope {
        private final List<TextPart> parts = new ArrayList<>();
        private final StringBuilder literal = new StringBuilder();

        void append(char c) {
            literal.append(c);
        }

        void append(String s) {
            literal.append(s);
        }

        void add(TextPart part) {
            flushLiteral();
            parts.add(part);
        }

        void flushLiteral() {
            if (literal.isEmpty()) return;
            parts.add(new TextPart.Literal(literal.toString()));
            literal.setLength(0);
        }

        List<TextPart> takeParts() {
            flushLiteral();
            return parts;
        }
    }
}