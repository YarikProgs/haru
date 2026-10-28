package net.aros.haru.diagnostic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record SourceSpan(SourcePos start, SourcePos end) {
    public static @NotNull SourceSpan dot(int line, int column) {
        var pos = new SourcePos(line, column);
        return new SourceSpan(pos, pos);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull SourceSpan selection(int line, int column, int len) {
        return new SourceSpan(new SourcePos(line, column), new SourcePos(line, column + len));
    }
}
