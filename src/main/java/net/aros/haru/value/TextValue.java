package net.aros.haru.value;

import net.aros.haru.ast.TextPart;
import net.aros.haru.parse.ParsedText;

public record TextValue(ParsedText parsedText) implements Value {
    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public TextPart asText() {
        return parsedText.text();
    }

    @Override
    public String asRawText() {
        return parsedText.raw();
    }
}
