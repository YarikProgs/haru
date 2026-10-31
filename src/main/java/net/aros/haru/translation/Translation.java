package net.aros.haru.translation;

import com.google.gson.*;
import net.aros.haru.parse.ParsedText;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record Translation(ParsedText parsedText, boolean fuzzy) {
    public static class Serializer implements JsonSerializer<Translation> {
        @Override
        public JsonElement serialize(@NotNull Translation src, Type typeOfSrc, JsonSerializationContext context) {
            if (!src.fuzzy()) return context.serialize(src.parsedText());
            JsonObject object = new JsonObject();
            object.add("value", context.serialize(src.parsedText()));
            object.add("fuzzy", new JsonPrimitive(true));
            return object;
        }
    }

    public static class Deserializer implements JsonDeserializer<Translation> {
        @Override
        public Translation deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive())
                return new Translation(context.deserialize(json, ParsedText.class), false);
            JsonObject object = json.getAsJsonObject();
            return new Translation(context.deserialize(object.get("value"), ParsedText.class), object.get("fuzzy").getAsBoolean());
        }
    }
}
