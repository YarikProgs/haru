package net.aros.haru.translation;

import net.aros.haru.parse.ParsedText;

import java.util.Map;
import java.util.Objects;

public record TextEntry(ParsedText original, Map<String, Translation> translations) {
    public TextEntry(ParsedText original, Map<String, Translation> translations) {
        this.original = Objects.requireNonNull(original, "'original' cannot be null");
        this.translations = translations == null ? Map.of() : translations;
    }
}
