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
        return new TextParser(new DiagnosticCollector(), null, token).parse();
    }

    private static TranslationFile merge(@Nullable TranslationFile oldFile, String... texts) {
        TranslationMerger merger = new TranslationMerger(oldFile);
        for (String text : texts) {
            merger.addText(parse(text));
        }
        return merger.merge();
    }

    /** Checked translation. */
    private static Translation tr(String text) {
        return new Translation(parse(text), false);
    }

    /** Translation that needs review. */
    private static Translation fuzzy(String text) {
        return new Translation(parse(text), true);
    }

    private static TextEntry entry(String original, Map<String, Translation> translations) {
        return new TextEntry(parse(original), translations);
    }

    private static TextEntry entry(String original) {
        return entry(original, Map.of());
    }

    private static Map<String, Translation> translations(Object... pairs) {
        Map<String, Translation> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put((String) pairs[i], (Translation) pairs[i + 1]);
        }
        return result;
    }

    @Test
    public void freshEntryHasNoTranslations() {
        TranslationFile file = merge(null, "Hello {name}!");
        Assertions.assertEquals(List.of(entry("Hello {name}!")), file.entries());
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void addedPhraseIsInsertedInTheMiddle() {
        TranslationFile oldFile = new TranslationFile(
                List.of(
                        entry("First", translations("ru_ru", tr("Первый"))),
                        entry("Third", translations("ru_ru", tr("Третий")))
                ),
                List.of()
        );

        TranslationFile file = merge(oldFile, "First", "Second", "Third");

        Assertions.assertEquals(
                List.of(
                        entry("First", translations("ru_ru", tr("Первый"))),
                        entry("Second"),
                        entry("Third", translations("ru_ru", tr("Третий")))
                ),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void deletedPhraseFallsIntoRemovedAndIsRestoredLater() {
        TranslationFile oldFile = new TranslationFile(
                List.of(
                        entry("A", translations("ru_ru", tr("А"))),
                        entry("B", translations("ru_ru", tr("Б")))
                ),
                List.of(entry("Old removed", translations("ru_ru", tr("Удалён и раньше"))))
        );

        // "B" is deleted -> falls into removed, old 'removed' is kept
        TranslationFile afterDeletion = merge(oldFile, "A");
        Assertions.assertEquals(List.of(entry("A", translations("ru_ru", tr("А")))), afterDeletion.entries());
        Assertions.assertEquals(
                List.of(
                        entry("Old removed", translations("ru_ru", tr("Удалён и раньше"))),
                        entry("B", translations("ru_ru", tr("Б")))
                ),
                afterDeletion.removed()
        );

        // "B" is back -> restored with its translations
        TranslationFile afterReturn = merge(afterDeletion, "A", "B");
        Assertions.assertEquals(
                List.of(
                        entry("A", translations("ru_ru", tr("А"))),
                        entry("B", translations("ru_ru", tr("Б")))
                ),
                afterReturn.entries()
        );
        Assertions.assertEquals(
                List.of(entry("Old removed", translations("ru_ru", tr("Удалён и раньше")))),
                afterReturn.removed()
        );
    }

    @Test
    public void deletedPhraseWithoutTranslationsIsDropped() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("A"), entry("B", translations("ru_ru", tr("Б")))),
                List.of()
        );

        TranslationFile file = merge(oldFile);

        Assertions.assertTrue(file.entries().isEmpty());
        Assertions.assertEquals(List.of(entry("B", translations("ru_ru", tr("Б")))), file.removed());
    }

    @Test
    public void unchangedPhraseKeepsTranslationsAndFuzzyFlags() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Hi", translations("ru_ru", tr("Привет"), "en_us", fuzzy("Hey")))),
                List.of()
        );

        TranslationFile file = merge(oldFile, "Hi");

        Assertions.assertEquals(
                List.of(entry("Hi", translations("ru_ru", tr("Привет"), "en_us", fuzzy("Hey")))),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void editedPhraseKeepsTranslationsButEverythingBecomesFuzzy() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Number that stands after <blue>{a}</blue> is <red>{b}</red>",
                        translations(
                                "ru_ru", tr("Число, стоящее после <blue>{a}</blue>, это <red>{b}</red>"),
                                "en_us", tr("Number that stands after <blue>{a}</blue> is <red>{b}</red>")
                        ))),
                List.of()
        );

        TranslationFile file = merge(oldFile,
                "The number that stands after <blue>{a}</blue> is <red>{b}</red>");

        Assertions.assertEquals(
                List.of(entry("The number that stands after <blue>{a}</blue> is <red>{b}</red>",
                        translations(
                                "ru_ru", fuzzy("Число, стоящее после <blue>{a}</blue>, это <red>{b}</red>"),
                                "en_us", fuzzy("Number that stands after <blue>{a}</blue> is <red>{b}</red>")
                        ))),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void editedPhraseWithoutTranslationsJustGetsNewOriginal() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Number that stands after <blue>{a}</blue> is <red>{b}</red>")),
                List.of()
        );

        TranslationFile file = merge(oldFile,
                "The number that stands after <blue>{a}</blue> is <red>{b}</red>");

        Assertions.assertEquals(
                List.of(entry("The number that stands after <blue>{a}</blue> is <red>{b}</red>")),
                file.entries()
        );
        Assertions.assertTrue(file.removed().isEmpty());
    }

    @Test
    public void unrelatedPhraseIsNotMatchedAsEdit() {
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Goodbye.", translations("ru_ru", tr("До свидания.")))),
                List.of()
        );

        TranslationFile file = merge(oldFile, "Hello.");

        Assertions.assertEquals(List.of(entry("Hello.")), file.entries());
        Assertions.assertEquals(
                List.of(entry("Goodbye.", translations("ru_ru", tr("До свидания.")))),
                file.removed()
        );
    }

    @Test
    public void samePhraseTwiceProducesSeparateEntries() {
        // fresh: each occurrence is its own entry
        TranslationFile file = merge(null, "Hi", "Hi");
        Assertions.assertEquals(List.of(entry("Hi"), entry("Hi")), file.entries());

        // with an old file only the first occurrence binds to the old entry,
        // the second is a fresh entry at its own position
        TranslationFile oldFile = new TranslationFile(
                List.of(entry("Hi", translations("ru_ru", tr("Привет")))),
                List.of()
        );
        TranslationFile merged = merge(oldFile, "Hi", "Hi");

        Assertions.assertEquals(
                List.of(entry("Hi", translations("ru_ru", tr("Привет"))), entry("Hi")),
                merged.entries()
        );
        Assertions.assertTrue(merged.removed().isEmpty());
    }
}