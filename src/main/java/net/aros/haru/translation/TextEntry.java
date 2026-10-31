package net.aros.haru.translation;

import net.aros.haru.parse.ParsedText;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TextEntry(ParsedText original, Map<String, ParsedText> translations, List<String> fuzzy) {
    public TextEntry(ParsedText original, Map<String, ParsedText> translations, List<String> fuzzy) {
        this.original = Objects.requireNonNull(original, "'original' cannot be null");
        this.translations = translations == null ? Map.of() : translations;
        this.fuzzy = fuzzy == null ? List.of() : fuzzy;
    }
}
