package net.aros.haru.translation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TextEntry(String original, Map<String, String> translations, List<String> fuzzy) {
    public TextEntry(String original, Map<String, String> translations, List<String> fuzzy) {
        this.original = Objects.requireNonNull(original, "'original' cannot be null");
        this.translations = translations == null ? Map.of() : translations;
        this.fuzzy = fuzzy == null ? List.of() : fuzzy;
    }
}
