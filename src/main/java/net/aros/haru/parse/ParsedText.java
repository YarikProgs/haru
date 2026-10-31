package net.aros.haru.parse;

import com.google.gson.*;
import net.aros.haru.Haru;
import net.aros.haru.ast.TextPart;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record ParsedText(String raw, TextPart text) {
    public static class Serializer implements JsonSerializer<ParsedText> {
        @Override
        public JsonElement serialize(@NotNull ParsedText src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.raw());
        }
    }

    public static class Deserializer implements JsonDeserializer<ParsedText> {
        @Override
        public ParsedText deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String raw = json.getAsString();
            DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
            ParsedText parsedText = Haru.parseText(Token.syntheticText(raw), diagnosticCollector, null);
            if (diagnosticCollector.hasErrors())
                return new ParsedText(raw, new TextPart.Literal(raw));
            return parsedText;
        }
    }
}
