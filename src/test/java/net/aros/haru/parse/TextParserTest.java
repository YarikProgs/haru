package net.aros.haru.parse;

import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.Severity;
import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TextParserTest {
    private static TextPart parse(DiagnosticCollector collector, String source) {
        Token token = new Token(TokenType.TEXT, new SourcePos(1, 1), "\"" + source + "\"", source);
        return new TextParser(collector, token).parse().text();
    }

    private static List<TextPart> parseParts(DiagnosticCollector collector, String source) {
        TextPart result = parse(collector, source);
        Assertions.assertInstanceOf(TextPart.Parts.class, result);
        return ((TextPart.Parts) result).parts();
    }

    private static void assertClean(DiagnosticCollector collector) {
        Assertions.assertFalse(collector.hasErrors());
        Assertions.assertTrue(collector.diagnostics().isEmpty(), "Expected no diagnostics, got: " + collector.diagnostics());
    }

    private static void assertHasSeverity(DiagnosticCollector collector, Severity severity, String messagePart) {
        Assertions.assertTrue(
                collector.diagnostics().stream()
                        .anyMatch(d -> d.severity() == severity && d.message().contains(messagePart)),
                "Expected " + severity + " containing '" + messagePart + "', got: " + collector.diagnostics()
        );
    }

    @Test
    public void plainLiteral() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "Hello world");
        Assertions.assertEquals(List.of(new TextPart.Literal("Hello world")), result);
        assertClean(collector);
    }

    @Test
    public void emptyStringReturnsEmptyLiteral() {
        DiagnosticCollector collector = new DiagnosticCollector();
        TextPart result = parse(collector, "");
        Assertions.assertEquals(new TextPart.Literal(""), result);
        assertClean(collector);
    }

    @Test
    public void placeholder() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "Hello {name}!");
        Assertions.assertEquals(
                List.of(new TextPart.Literal("Hello "), new TextPart.Placeholder("name"), new TextPart.Literal("!")),
                result
        );
        assertClean(collector);
    }

    @Test
    public void placeholderWithSurroundingSpaces() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "{ name }");
        Assertions.assertEquals(List.of(new TextPart.Placeholder("name")), result);
        assertClean(collector);
    }

    @Test
    public void emptyPlaceholderIsError() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "a{}b");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Empty placeholder");
        Assertions.assertTrue(result.stream().noneMatch(p -> p instanceof TextPart.Placeholder));
    }

    @Test
    public void invalidPlaceholderIsError() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "{a b}");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Invalid placeholder name");
        Assertions.assertTrue(result.stream().noneMatch(p -> p instanceof TextPart.Placeholder));
    }

    @Test
    public void unterminatedPlaceholder() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "{name");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Unterminated placeholder");
        Assertions.assertEquals(List.of(new TextPart.Placeholder("name")), result);
    }

    @Test
    public void unterminatedPlaceholderPosition() {
        DiagnosticCollector collector = new DiagnosticCollector();
        parse(collector, "a {b");
        Assertions.assertTrue(collector.hasErrors());
        Assertions.assertEquals(1, collector.diagnostics().getFirst().pos().line());
        Assertions.assertEquals(3, collector.diagnostics().getFirst().pos().column());
    }

    @Test
    public void escapedBracesAreLiteral() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "\\{name}");
        Assertions.assertEquals(List.of(new TextPart.Literal("{name}")), result);
        assertClean(collector);
    }

    @Test
    public void escapedBracketIsLiteral() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "a\\<b");
        Assertions.assertEquals(List.of(new TextPart.Literal("a<b")), result);
        assertClean(collector);
    }

    @Test
    public void selfClosingTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<wait 2s/>");
        Assertions.assertEquals(List.of(new TextPart.Tag("wait", List.of("2s"))), result);
        assertClean(collector);
    }

    @Test
    public void selfClosingTagWithoutArguments() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<br/>");
        Assertions.assertEquals(List.of(new TextPart.Tag("br", List.of())), result);
        assertClean(collector);
    }

    @Test
    public void pairedTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<red>сюда</red>");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("red", List.of(), new TextPart.Literal("сюда"))),
                result
        );
        assertClean(collector);
    }

    @Test
    public void placeholderInsidePairedTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<red>{name}</red>");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("red", List.of(), new TextPart.Placeholder("name"))),
                result
        );
        assertClean(collector);
    }

    @Test
    public void nestedPairedTags() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<a><b>x</b></a>");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("a", List.of(),
                        new TextPart.BiTag("b", List.of(), new TextPart.Literal("x")))),
                result
        );
        assertClean(collector);
    }

    @Test
    public void multiplePartsInsidePairedTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<red>hello {name}</red>");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("red", List.of(), new TextPart.Parts(List.of(
                        new TextPart.Literal("hello "),
                        new TextPart.Placeholder("name")
                )))),
                result
        );
        assertClean(collector);
    }

    @Test
    public void mismatchedClosingTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        parse(collector, "<a>x</b>");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Mismatched closing tag");
    }

    @Test
    public void unexpectedClosingTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "x</br>");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Unexpected closing tag");
        Assertions.assertEquals(List.of(new TextPart.Literal("x")), result);
    }

    @Test
    public void unclosedTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<red>hello");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Unclosed tag");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("red", List.of(), new TextPart.Literal("hello"))),
                result
        );
    }

    @Test
    public void unterminatedTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<wait");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Unterminated tag");
        Assertions.assertEquals(List.of(new TextPart.Tag("wait", List.of())), result);
    }

    @Test
    public void bareLessThanIsLiteralWithError() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "a < b");
        Assertions.assertTrue(collector.hasErrors());
        assertHasSeverity(collector, Severity.ERROR, "Invalid tag syntax");
        Assertions.assertEquals(List.of(new TextPart.Literal("a < b")), result);
    }

    @Test
    public void strayClosingBraceIsLiteral() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "a } b");
        Assertions.assertEquals(List.of(new TextPart.Literal("a } b")), result);
        assertClean(collector);
    }

    @Test
    public void emptyPairedTagWarns() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "<red></red>");
        Assertions.assertFalse(collector.hasErrors());
        assertHasSeverity(collector, Severity.WARNING, "Empty paired tag");
        Assertions.assertEquals(
                List.of(new TextPart.BiTag("red", List.of(), new TextPart.Literal(""))),
                result
        );
    }

    @Test
    public void firstHaruPlaceholdersAndBiTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "{company_cap}, проходите <red>сюда</red>");
        Assertions.assertEquals(
                List.of(
                        new TextPart.Placeholder("company_cap"),
                        new TextPart.Literal(", проходите "),
                        new TextPart.BiTag("red", List.of(), new TextPart.Literal("сюда"))
                ),
                result
        );
        assertClean(collector);
    }

    @Test
    public void firstHaruSelfClosingTag() {
        DiagnosticCollector collector = new DiagnosticCollector();
        List<TextPart> result = parseParts(collector, "Медлите.<wait 2s/> Быстро в зал");
        Assertions.assertEquals(
                List.of(
                        new TextPart.Literal("Медлите."),
                        new TextPart.Tag("wait", List.of("2s")),
                        new TextPart.Literal(" Быстро в зал")
                ),
                result
        );
        assertClean(collector);
    }
}
