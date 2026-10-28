package net.aros.haru.translation;

import java.util.List;

public record TranslationFile(List<TextEntry> entries, List<TextEntry> removed) {
    public TranslationFile(List<TextEntry> entries, List<TextEntry> removed) {
        this.entries = entries == null ? List.of() : entries;
        this.removed = removed == null ? List.of() : removed;
    }
}
