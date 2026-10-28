package net.aros.haru.translation;

import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.parse.TextParser;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TranslationMergerTest {
    private static ParsedText parse(String source) {
        Token token = new Token(TokenType.TEXT, SourceSpan.dot(1, 1), "\"" + source + "\"", source);
        return new TextParser(new DiagnosticCollector(), token).parse();
    }

    private static TranslationFile merge(@Nullable TranslationFile oldFile, List<String> locales, String... texts) {
        TranslationMerger merger = new TranslationMerger(oldFile);
        merger.withLocales(locales);
        for (String text : texts) {
            merger.addText(parse(text));
        }
        return merger.merge();
    }

    private static TextEntry entry(String original, Map<String, String> translations, String... fuzzy) {
        return new TextEntry(original, translations, List.of(fuzzy));
    }

    private static TextEntry entry(String original) {
        return entry(original, Map.<String, String>of());
    }

    private static Map<String, String> translations(String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    @Test
    public void freshEntryGetsCopiedOriginalsAsFuzzyTranslations() {
        TranslationFile file = merge(null, List.of("ru_ru", "en_us"), "Hello {name}!");
        Assertions.assertEquals(
                List.of(entry("Hello {name}!", Map.of("ru_ru", "Hello {name}!", "en_us", "Hello {name}!"), "ru_ru", "en_us")),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void addedPhraseIsInsertedInTheMiddle() {
        TranslationFile oldFile = new TranslationFile(
                List.of(
                        entry("First", Map.of("ru_ru", "Первый")),
                        entry("Third", Map.of("ru_ru", "Третий"))
                ),
                List.of()
        );

        TranslationFile file = merge(oldFile, List.of(), "First", "Second", "Third");

        Assertions.assertEquals(
                List.of(
                        entry("First", Map.of("ru_ru", "Первый")),
                        entry("Second"),
                        entry("Third", Map.of("ru_ru", "Третий"))
                ),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void deletedPhraseFallsIntoRemovedAndIsRestoredLater() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("A", Map.of("ru_ru", "А")), entry("B", Map.of("ru_ru", "Б"))),
                List.of(entry("Old removed", Map.of("ru_ru", "Удалён и раньше")))
        );

        // "B" is deleted -> falls into removed, old 'removed' is kept
        TranslationFile afterDeletion = merge(oldFile, List.of(), "A");
        Assertions.assertEquals(List.of(entry("A", Map.of("ru_ru", "А"))), afterDeletion.entries());
        Assertions.assertEquals(
                List.of(entry("Old removed", Map.of("ru_ru", "Удалён и раньше")), entry("B", Map.of("ru_ru", "Б"))),
                afterDeletion.removed()
        );

        // "B" is back -> restored with its translations
        TranslationFile afterReturn = merge(afterDeletion, List.of(), "A", "B");
        Assertions.assertEquals(
                List.of(entry("A", Map.of("ru_ru", "А")), entry("B", Map.of("ru_ru", "Б"))),
                afterReturn.entries()
        );
        Assertions.assertEquals(List.of(entry("Old removed", Map.of("ru_ru", "Удалён и раньше"))), afterReturn.removed());
    }

    @Test
    public void editedPhraseKeepsTranslationsButEverythingBecomesFuzzy() {
        TranslationFile oldFile = new TranslationFile(
                List.of(new TextEntry("Number that stands after <blue>{a}</blue> is <red>{b}</red>",
                        translations(
                                "ru_ru", "Число, стоящее после <blue>{a}</blue>, это <red>{b}</red>",
                                "en_us", "Number that stands after <blue>{a}</blue> is <red>{b}</red>"
                        ),
                        List.of())),
                List.of()
        );

        TranslationFile file = merge(oldFile, List.of("ru_ru", "en_us"),
                "The number that stands after <blue>{a}</blue> is <red>{b}</red>");

        Assertions.assertEquals(
                List.of(new TextEntry("The number that stands after <blue>{a}</blue> is <red>{b}</red>",
                        translations(
                                "ru_ru", "Число, стоящее после <blue>{a}</blue>, это <red>{b}</red>",
                                "en_us", "Number that stands after <blue>{a}</blue> is <red>{b}</red>"
                        ),
                        List.of("ru_ru", "en_us"))),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void missingLocaleIsFilledWithOriginalAndMarkedFuzzy() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Hi", Map.of("ru_ru", "Привет"))),
                List.of()
        );

        TranslationFile file = merge(oldFile, List.of("ru_ru", "en_us"), "Hi");

        Assertions.assertEquals(
                List.of(entry("Hi", Map.of("ru_ru", "Привет", "en_us", "Hi"), "en_us")),
                file.entries()
        );
    }

    @Test
    public void unrelatedPhraseIsNotMatchedAsEdit() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Goodbye.", Map.of("ru_ru", "До свидания."))),
                List.of()
        );

        TranslationFile file = merge(oldFile, List.of(), "Hello.");

        Assertions.assertEquals(List.of(entry("Hello.")), file.entries());
        Assertions.assertEquals(List.of(entry("Goodbye.", Map.of("ru_ru", "До свидания."))), file.removed());
    }

    @Test
    public void samePhraseTwiceProducesSeparateEntries() {
        // fresh: each occurrence is its own entry
        TranslationFile file = merge(null, List.of(), "Hi", "Hi");
        Assertions.assertEquals(List.of(entry("Hi"), entry("Hi")), file.entries());

        // with an old file only the first occurrence binds to the old entry,
        // the second is a fresh entry at its own position
        TranslationFile oldFile = new TranslationFile(List.of(entry("Hi", Map.of("ru_ru", "Привет"))), List.of());
        TranslationFile merged = merge(oldFile, List.of(), "Hi", "Hi");

        Assertions.assertEquals(
                List.of(entry("Hi", Map.of("ru_ru", "Привет")), entry("Hi")),
                merged.entries()
        );
        Assertions.assertTrue(merged.removed().isEmpty());
    }
}
