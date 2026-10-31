package net.aros.haru.translation;

import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.parse.TextParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migrates a translation file to a new version of the source.
 * <p>
 * Texts are collected from the new source with {@link #addText(ParsedText)} in source order,
 * then {@link #merge()} rebuilds the translation file, preserving as much of the old file as possible:
 * <ul>
 *     <li>every text occurrence gets its own entry - even if the phrase repeats in the source;</li>
 *     <li>entries are rebuilt in source order, so a phrase added in the middle is inserted
 *     in the middle of the file and the following phrases are shifted down;</li>
 *     <li>a new phrase gets an entry with no translations: at runtime the original is used
 *     until an editor adds a locale;</li>
 *     <li>an unchanged phrase keeps its translations (including their fuzzy flags);</li>
 *     <li>a deleted phrase that had translations falls into {@code removed}
 *     (entries without translations have nothing to preserve and are dropped);</li>
 *     <li>a phrase that returns after deletion is restored from {@code removed} with its translations;</li>
 *     <li>an edited phrase (same placeholders and tags, similar literals) keeps its translations,
 *     but gets the new original and every translation becomes fuzzy.</li>
 * </ul>
 */
public final class TranslationMerger {
    /**
     * Minimal similarity of the raw texts for an edited phrase to be recognized
     * as a change of an existing entry instead of deletion + addition.
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final List<TextEntry> oldEntries;
    private final List<TextEntry> oldRemoved;
    private final List<Collected> texts = new ArrayList<>();

    public TranslationMerger(@Nullable TranslationFile oldTranslationFile) {
        this.oldEntries = oldTranslationFile == null ? new ArrayList<>() : new ArrayList<>(oldTranslationFile.entries());
        this.oldRemoved = oldTranslationFile == null ? new ArrayList<>() : new ArrayList<>(oldTranslationFile.removed());
    }

    /**
     * Registers a text from the new source.
     * @param text parsed text (double-quoted string)
     */
    public void addText(@NotNull ParsedText text) {
        texts.add(new Collected(text, signatureOf(text)));
    }

    public @NotNull TranslationFile merge() {
        TextEntry[] resolved = new TextEntry[texts.size()];

        // Pass 1: exact matches. They run first, so that an unchanged phrase always binds
        // to its own old entry before any structural match can take it away.
        for (int i = 0; i < texts.size(); i++) {
            Collected text = texts.get(i);
            TextEntry entry = takeExact(oldEntries, text.raw());
            if (entry == null) entry = takeExact(oldRemoved, text.raw()); // the phrase is back
            if (entry != null) resolved[i] = entry;
        }

        // Pass 2: edited phrases. Structure (placeholders and tags) is the same, literals differ.
        for (int i = 0; i < texts.size(); i++) {
            Collected text = texts.get(i);
            if (resolved[i] != null) continue;
            TextEntry entry = takeSimilar(oldEntries, text);
            if (entry == null) entry = takeSimilar(oldRemoved, text);
            if (entry == null) continue;
            resolved[i] = markAllFuzzy(entry, text.raw());
        }

        // Entries are rebuilt in source order: added phrases take their place, the rest is shifted.
        // Every text occurrence gets its own entry, even if the phrase repeats in the source.
        List<TextEntry> entries = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            TextEntry entry = resolved[i];
            if (entry == null) entry = new TextEntry(texts.get(i).raw(), new LinkedHashMap<>());
            entries.add(entry);
        }

        // Everything old that was not matched is considered removed,
        // but only if it carries translations worth keeping.
        // Old 'removed' comes first, so it keeps its position across compilations.
        List<TextEntry> removed = new ArrayList<>(oldRemoved);
        Set<ParsedText> removedOriginals = new HashSet<>();
        for (TextEntry entry : removed) removedOriginals.add(entry.original());
        for (TextEntry entry : oldEntries) {
            if (entry.translations().isEmpty()) continue;
            if (removedOriginals.add(entry.original())) removed.add(entry);
        }
        return new TranslationFile(List.copyOf(entries), List.copyOf(removed));
    }

    @Nullable
    private TextEntry takeExact(List<TextEntry> pool, ParsedText raw) {
        for (Iterator<TextEntry> it = pool.iterator(); it.hasNext(); ) {
            TextEntry entry = it.next();
            if (entry.original().equals(raw)) {
                it.remove();
                return entry;
            }
        }
        return null;
    }

    /**
     * Takes the entry that is the best candidate for an edited version of the text:
     * same structure (placeholders and tags) and similar enough literals.
     */
    @Nullable
    private TextEntry takeSimilar(List<TextEntry> pool, Collected text) {
        TextEntry best = null;
        int bestIndex = -1;
        double bestSimilarity = SIMILARITY_THRESHOLD;
        for (int i = 0; i < pool.size(); i++) {
            TextEntry entry = pool.get(i);
            if (!signatureOf(entry.original()).equals(text.signature())) continue;
            double similarity = similarity(text.raw().raw(), entry.original().raw());
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                best = entry;
                bestIndex = i;
            }
        }
        if (best != null) pool.remove(bestIndex);
        return best;
    }

    /**
     * The phrase was edited: writes the new original and makes every translation fuzzy.
     * An entry without translations simply gets the new original.
     */
    private @NotNull TextEntry markAllFuzzy(@NotNull TextEntry entry, @NotNull ParsedText newOriginal) {
        Map<String, Translation> translations = new LinkedHashMap<>();
        for (Map.Entry<String, Translation> e : entry.translations().entrySet()) {
            translations.put(e.getKey(), new Translation(e.getValue().parsedText(), true));
        }
        return new TextEntry(newOriginal, translations);
    }

    private static double similarity(@NotNull String a, @NotNull String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        return 1.0 - (double) levenshtein(a, b) / Math.max(a.length(), b.length());
    }

    private static int levenshtein(@NotNull String a, @NotNull String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                curr[j] = Math.min(substitution, Math.min(prev[j] + 1, curr[j - 1] + 1));
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }

    /**
     * Structure of a text: the sequence of placeholders and tags with their names and arguments.
     * Literal contents are ignored, so an edited phrase keeps the signature of its old version.
     */
    private static String signatureOf(@NotNull ParsedText parsedText) {
        String raw = parsedText.raw();
        // Old entries store only the raw text, so it is parsed again the same way TextParserTest does it
        Token token = new Token(TokenType.TEXT, SourceSpan.dot(1, 1), "\"" + raw + "\"", raw);
        return signature(new TextParser(new DiagnosticCollector(), null, token).parse().text());
    }

    private static String signature(@NotNull TextPart part) {
        StringBuilder sb = new StringBuilder();
        appendSignature(part, sb);
        return sb.toString();
    }

    private static void appendSignature(@NotNull TextPart part, @NotNull StringBuilder sb) {
        switch (part) {
            case TextPart.Literal ignored -> {} // literal contents may be freely edited
            case TextPart.Placeholder placeholder -> sb.append('{').append(placeholder.name()).append('}');
            case TextPart.Tag tag -> sb.append('<').append(tag.tag()).append(tag.arguments()).append("/>");
            case TextPart.BiTag biTag -> {
                sb.append('<').append(biTag.tag()).append(biTag.arguments()).append('>');
                appendSignature(biTag.inner(), sb);
                sb.append("</>");
            }
            case TextPart.Parts parts -> {
                for (TextPart child : parts.parts()) appendSignature(child, sb);
            }
        }
    }

    private record Collected(ParsedText raw, String signature) {
    }
}