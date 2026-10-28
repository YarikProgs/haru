package net.aros.haru.lex;

import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifierCollector {
    private final Map<String, List<SourceSpan>> occurrences = new HashMap<>();

    public void add(String identifier, SourceSpan span) {
        occurrences.computeIfAbsent(identifier, ignored -> new ArrayList<>()).add(span);
    }

    public List<SourceSpan> occurrencesOf(String identifier) {
        return occurrences.getOrDefault(identifier, List.of());
    }

    @Nullable
    public Occurrence occurrenceAt(int line, int column) {
        for (Map.Entry<String, List<SourceSpan>> entry : occurrences.entrySet()) {
            for (SourceSpan span : entry.getValue()) {
                if (touches(span, line, column))
                    return new Occurrence(entry.getKey(), span, occurrencesOf(entry.getKey()));
            }
        }
        return null;
    }

    private boolean touches(@NotNull SourceSpan span, int line, int column) {
        return compare(span.start(), line, column) <= 0 && compare(span.end(), line, column) >= 0;
    }

    private int compare(SourcePos pos, int line, int column) {
        if (pos.line() != line) return Integer.compare(pos.line(), line);
        return Integer.compare(pos.column(), column);
    }

    public record Occurrence(String identifier, SourceSpan at, List<SourceSpan> all) {
    }
}
