package net.aros.haru.diagnostic;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public final class DiagnosticRenderer {
    private DiagnosticRenderer() {}

    private static final String RESET = "\033[0m";
    private static final String RED = "\033[31;1m";
    private static final String YELLOW = "\033[33;1m";
    private static final String CYAN = "\033[36;1m";
    private static final String DIM = "\033[2m";
    private static final String BOLD = "\033[1m";

    public static @NotNull String render(@NotNull List<Diagnostic> diagnostics, Path path, @NotNull String source) {
        String[] lines = source.split("\\R", -1);
        int gutter = Integer.toString(lines.length).length();

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < diagnostics.size(); i++) {
            Diagnostic d = diagnostics.get(i);
            String color = switch (d.severity()) {
                case ERROR -> RED;
                case WARNING -> YELLOW;
                case HINT -> CYAN;
            };
            String label = d.severity().name().toLowerCase();

            out.append(color).append(BOLD)
                .append(label).append(RESET)
                .append(": ").append(d.message()).append('\n');

            SourceSpan span = d.span();
            if (span != null) {
                SourcePos start = span.start();
                SourcePos end = span.end();

                out.append(DIM).append("  --> ").append(RESET)
                    .append(path).append(':')
                    .append(start.line()).append(':')
                    .append(start.column()).append('\n');

                int lineIndex = start.line() - 1;
                if (lineIndex >= 0 && lineIndex < lines.length) {
                    String line = lines[lineIndex];
                    String expanded = expandTabs(line);

                    int column = Math.clamp(start.column() - 1, 0, line.length());
                    int visualColumn = expandTabs(line.substring(0, column)).length();

                    int endColumn = start.line() == end.line()
                        ? Math.clamp(end.column() - 1, column, line.length())
                        : line.length();

                    int width = Math.max(1,
                        expandTabs(line.substring(column, endColumn)).length());

                    String number = Integer.toString(start.line());
                    String padding = " ".repeat(gutter - number.length());

                    out.append(DIM).append(padding).append(number)
                        .append(" | ").append(RESET)
                        .append(expanded).append('\n');

                    out.repeat(" ", gutter)
                        .append(DIM).append(" | ").append(RESET)
                        .repeat(" ", visualColumn)
                        .append(color).append("^")
                        .repeat("~", Math.max(0, width - 1))
                        .append(RESET).append('\n');
                }
            }

            if (i + 1 < diagnostics.size()) {
                out.append('\n');
            }
        }

        return out.toString();
    }

    private static @NotNull String expandTabs(@NotNull String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\t') {
                result.repeat(" ", 4 - result.length() % 4);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
