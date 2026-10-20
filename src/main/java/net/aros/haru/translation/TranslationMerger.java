package net.aros.haru.translation;

import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.parse.TextParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 *     <li>an unchanged phrase keeps its translations and fuzzy locales;</li>
 *     <li>a deleted phrase falls into {@code removed};</li>
 *     <li>a phrase that returns after deletion is restored from {@code removed} with its translations;</li>
 *     <li>an edited phrase (same placeholders and tags, similar literals) keeps its translations,
 *     but gets the new original and every locale of the entry falls into {@code fuzzy};</li>
 *     <li>locales listed in {@link #withLocales(Collection)} are always present in {@code translations}:
 *     a missing one is filled with a copy of the original and marked fuzzy.</li>
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
    private final Set<String> locales = new LinkedHashSet<>();

    public TranslationMerger(@Nullable TranslationFile oldTranslationFile) {
        this.oldEntries = oldTranslationFile == null ? new ArrayList<>() : new ArrayList<>(oldTranslationFile.entries());
        this.oldRemoved = oldTranslationFile == null ? new ArrayList<>() : new ArrayList<>(oldTranslationFile.removed());
    }

    /**
     * Locales that must always be present in the 'translations' map of every entry.
     * A missing locale is filled with a copy of the original and added to 'fuzzy'.
     */
    public TranslationMerger withLocales(@NotNull Collection<String> locales) {
        this.locales.addAll(locales);
        return this;
    }

    /**
     * Registers a text from the new source.
     * @param text parsed text (double-quoted string)
     */
    public void addText(@NotNull ParsedText text) {
        texts.add(new Collected(text.raw(), signatureOf(text.raw())));
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
            if (entry == null) entry = new TextEntry(texts.get(i).raw(), new LinkedHashMap<>(), new ArrayList<>());
            entries.add(fillMissingLocales(entry, texts.get(i).raw()));
        }

        // Everything old that was not matched is considered removed.
        // Old 'removed' comes first, so it keeps its position across compilations.
        List<TextEntry> removed = new ArrayList<>(oldRemoved);
        Set<String> removedOriginals = new HashSet<>();
        for (TextEntry entry : removed) removedOriginals.add(entry.original());
        for (TextEntry entry : oldEntries) {
            if (removedOriginals.add(entry.original())) removed.add(entry);
        }
        return new TranslationFile(List.copyOf(entries), List.copyOf(removed));
    }

    @Nullable
    private TextEntry takeExact(List<TextEntry> pool, String raw) {
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
            double similarity = similarity(text.raw(), entry.original());
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
     * The phrase was edited: writes the new original and makes every locale of the entry fuzzy.
     */
    private @NotNull TextEntry markAllFuzzy(@NotNull TextEntry entry, @NotNull String newOriginal) {
        Map<String, String> translations = new LinkedHashMap<>(entry.translations());
        List<String> fuzzy = new ArrayList<>(entry.fuzzy());
        for (String locale : translations.keySet()) {
            if (!fuzzy.contains(locale)) fuzzy.add(locale);
        }
        return new TextEntry(newOriginal, translations, fuzzy);
    }

    /**
     * Adds every configured locale missing in 'translations' as a copy of the original, marking it fuzzy.
     */
    private @NotNull TextEntry fillMissingLocales(@NotNull TextEntry entry, @NotNull String original) {
        if (locales.isEmpty()) return entry;
        Map<String, String> translations = new LinkedHashMap<>(entry.translations());
        List<String> fuzzy = new ArrayList<>(entry.fuzzy());
        boolean changed = false;
        for (String locale : locales) {
            if (translations.containsKey(locale)) continue;
            translations.put(locale, original);
            if (!fuzzy.contains(locale)) fuzzy.add(locale);
            changed = true;
        }
        return changed ? new TextEntry(entry.original(), translations, fuzzy) : entry;
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
    private static String signatureOf(@NotNull String raw) {
        // Old entries store only the raw text, so it is parsed again the same way TextParserTest does it
        Token token = new Token(TokenType.TEXT, new SourcePos(1, 1), "\"" + raw + "\"", raw);
        return signature(new TextParser(new DiagnosticCollector(), token).parse().text());
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

    private record Collected(String raw, String signature) {
    }
}
