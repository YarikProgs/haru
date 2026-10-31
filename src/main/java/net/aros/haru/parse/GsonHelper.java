package net.aros.haru.parse;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class GsonHelper {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Does the given JsonObject contain a string field with the given name?
     */
    public static boolean isStringValue(JsonObject json, String memberName) {
        return !isValidPrimitive(json, memberName) ? false : json.getAsJsonPrimitive(memberName).isString();
    }

    /**
     * Is the given JsonElement a string?
     */
    public static boolean isStringValue(JsonElement json) {
        return !json.isJsonPrimitive() ? false : json.getAsJsonPrimitive().isString();
    }

    public static boolean isNumberValue(JsonObject json, String memberName) {
        return !isValidPrimitive(json, memberName) ? false : json.getAsJsonPrimitive(memberName).isNumber();
    }

    public static boolean isNumberValue(JsonElement json) {
        return !json.isJsonPrimitive() ? false : json.getAsJsonPrimitive().isNumber();
    }

    public static boolean isBooleanValue(JsonObject json, String memberName) {
        return !isValidPrimitive(json, memberName) ? false : json.getAsJsonPrimitive(memberName).isBoolean();
    }

    public static boolean isBooleanValue(JsonElement json) {
        return !json.isJsonPrimitive() ? false : json.getAsJsonPrimitive().isBoolean();
    }

    /**
     * Does the given JsonObject contain an array field with the given name?
     */
    public static boolean isArray(JsonObject json, String memberName) {
        return !isValidNode(json, memberName) ? false : json.get(memberName).isJsonArray();
    }

    public static boolean isObjectNode(JsonObject json, String memberName) {
        return !isValidNode(json, memberName) ? false : json.get(memberName).isJsonObject();
    }

    /**
     * Does the given JsonObject contain a field with the given name whose type is primitive (String, Java primitive, or Java primitive wrapper)?
     */
    public static boolean isValidPrimitive(JsonObject json, String memberName) {
        return !isValidNode(json, memberName) ? false : json.get(memberName).isJsonPrimitive();
    }

    /**
     * Does the given JsonObject contain a field with the given name?
     */
    public static boolean isValidNode(@Nullable JsonObject json, String memberName) {
        return json == null ? false : json.get(memberName) != null;
    }

    public static JsonElement getNonNull(JsonObject json, String memberName) {
        JsonElement jsonelement = json.get(memberName);
        if (jsonelement != null && !jsonelement.isJsonNull()) {
            return jsonelement;
        } else {
            throw new JsonSyntaxException("Missing field " + memberName);
        }
    }

    /**
     * Gets the string value of the given JsonElement.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static String convertToString(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) {
            return json.getAsString();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a string, was " + json);
        }
    }

    /**
     * Gets the string value of the field on the JsonObject with the given name.
     */
    public static String getAsString(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToString(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a string");
        }
    }

    /**
     * Gets the string value of the field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static String getAsString(JsonObject json, String memberName, @Nullable String fallback) {
        return json.has(memberName) ? convertToString(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets the boolean value of the given JsonElement.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static boolean convertToBoolean(JsonElement json, String memberName) {
        if (json.isJsonPrimitive()) {
            return json.getAsBoolean();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Boolean, was " + json);
        }
    }

    /**
     * Gets the boolean value of the field on the JsonObject with the given name.
     */
    public static boolean getAsBoolean(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToBoolean(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Boolean");
        }
    }

    /**
     * Gets the boolean value of the field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    public static boolean getAsBoolean(JsonObject json, String memberName, boolean fallback) {
        return json.has(memberName) ? convertToBoolean(json.get(memberName), memberName) : fallback;
    }

    public static double convertToDouble(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsDouble();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Double, was " + json);
        }
    }

    public static double getAsDouble(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToDouble(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Double");
        }
    }

    public static double getAsDouble(JsonObject json, String memberName, double fallback) {
        return json.has(memberName) ? convertToDouble(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets the float value of the given JsonElement.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static float convertToFloat(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsFloat();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Float, was " + json);
        }
    }

    /**
     * Gets the float value of the field on the JsonObject with the given name.
     */
    public static float getAsFloat(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToFloat(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Float");
        }
    }

    /**
     * Gets the float value of the field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    public static float getAsFloat(JsonObject json, String memberName, float fallback) {
        return json.has(memberName) ? convertToFloat(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets a long from a JSON element and validates that the value is actually a number.
     */
    public static long convertToLong(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsLong();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Long, was " + json);
        }
    }

    /**
     * Gets a long from a JSON element, throws an error if the member does not exist.
     */
    public static long getAsLong(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToLong(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Long");
        }
    }

    public static long getAsLong(JsonObject json, String memberName, long fallback) {
        return json.has(memberName) ? convertToLong(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets the integer value of the given JsonElement.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static int convertToInt(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsInt();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Int, was " + json);
        }
    }

    /**
     * Gets the integer value of the field on the JsonObject with the given name.
     */
    public static int getAsInt(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToInt(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Int");
        }
    }

    /**
     * Gets the integer value of the field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    public static int getAsInt(JsonObject json, String memberName, int fallback) {
        return json.has(memberName) ? convertToInt(json.get(memberName), memberName) : fallback;
    }

    public static byte convertToByte(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsByte();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Byte, was " + json);
        }
    }

    public static byte getAsByte(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToByte(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Byte");
        }
    }

    public static byte getAsByte(JsonObject json, String memberName, byte fallback) {
        return json.has(memberName) ? convertToByte(json.get(memberName), memberName) : fallback;
    }

    public static char convertToCharacter(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsCharacter();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Character, was " + json);
        }
    }

    public static char getAsCharacter(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToCharacter(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Character");
        }
    }

    public static char getAsCharacter(JsonObject json, String memberName, char fallback) {
        return json.has(memberName) ? convertToCharacter(json.get(memberName), memberName) : fallback;
    }

    public static BigDecimal convertToBigDecimal(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsBigDecimal();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a BigDecimal, was " + json);
        }
    }

    public static BigDecimal getAsBigDecimal(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToBigDecimal(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a BigDecimal");
        }
    }

    public static BigDecimal getAsBigDecimal(JsonObject json, String memberName, BigDecimal fallback) {
        return json.has(memberName) ? convertToBigDecimal(json.get(memberName), memberName) : fallback;
    }

    public static BigInteger convertToBigInteger(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsBigInteger();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a BigInteger, was " + json);
        }
    }

    public static BigInteger getAsBigInteger(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToBigInteger(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a BigInteger");
        }
    }

    public static BigInteger getAsBigInteger(JsonObject json, String memberName, BigInteger fallback) {
        return json.has(memberName) ? convertToBigInteger(json.get(memberName), memberName) : fallback;
    }

    public static short convertToShort(JsonElement json, String memberName) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            return json.getAsShort();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a Short, was " + json);
        }
    }

    public static short getAsShort(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToShort(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a Short");
        }
    }

    public static short getAsShort(JsonObject json, String memberName, short fallback) {
        return json.has(memberName) ? convertToShort(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets the given JsonElement as a JsonObject.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static JsonObject convertToJsonObject(JsonElement json, String memberName) {
        if (json.isJsonObject()) {
            return json.getAsJsonObject();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a JsonObject, was " + json);
        }
    }

    public static JsonObject getAsJsonObject(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToJsonObject(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonObject");
        }
    }

    /**
     * Gets the JsonObject field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static JsonObject getAsJsonObject(JsonObject json, String memberName, @Nullable JsonObject fallback) {
        return json.has(memberName) ? convertToJsonObject(json.get(memberName), memberName) : fallback;
    }

    /**
     * Gets the given JsonElement as a JsonArray.  Expects the second parameter to be the name of the element's field if an error message needs to be thrown.
     */
    public static JsonArray convertToJsonArray(JsonElement json, String memberName) {
        if (json.isJsonArray()) {
            return json.getAsJsonArray();
        } else {
            throw new JsonSyntaxException("Expected " + memberName + " to be a JsonArray, was " + json);
        }
    }

    /**
     * Gets the JsonArray field on the JsonObject with the given name.
     */
    public static JsonArray getAsJsonArray(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return convertToJsonArray(json.get(memberName), memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonArray");
        }
    }

    /**
     * Gets the JsonArray field on the JsonObject with the given name, or the given default value if the field is missing.
     */
    @Nullable
    @Contract("_,_,!null->!null;_,_,null->_")
    public static JsonArray getAsJsonArray(JsonObject json, String memberName, @Nullable JsonArray fallback) {
        return json.has(memberName) ? convertToJsonArray(json.get(memberName), memberName) : fallback;
    }

    public static <T> T convertToObject(@Nullable JsonElement json, String memberName, JsonDeserializationContext context, Class<? extends T> adapter) {
        if (json != null) {
            return context.deserialize(json, adapter);
        } else {
            throw new JsonSyntaxException("Missing " + memberName);
        }
    }

    public static <T> T getAsObject(JsonObject json, String memberName, JsonDeserializationContext context, Class<? extends T> adapter) {
        if (json.has(memberName)) {
            return convertToObject(json.get(memberName), memberName, context, adapter);
        } else {
            throw new JsonSyntaxException("Missing " + memberName);
        }
    }

    @Nullable
    @Contract("_,_,!null,_,_->!null;_,_,null,_,_->_")
    public static <T> T getAsObject(
            JsonObject json, String memberName, @Nullable T fallback, JsonDeserializationContext context, Class<? extends T> adapter
    ) {
        return json.has(memberName) ? convertToObject(json.get(memberName), memberName, context, adapter) : fallback;
    }

    @Nullable
    public static <T> T fromNullableJson(Gson gson, Reader reader, Class<T> adapter, boolean lenient) {
        try {
            JsonReader jsonreader = new JsonReader(reader);
            jsonreader.setLenient(lenient);
            return gson.getAdapter(adapter).read(jsonreader);
        } catch (IOException ioexception) {
            throw new JsonParseException(ioexception);
        }
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> adapter, boolean lenient) {
        T t = fromNullableJson(gson, reader, adapter, lenient);
        if (t == null) {
            throw new JsonParseException("JSON data was null or empty");
        } else {
            return t;
        }
    }

    @Nullable
    public static <T> T fromNullableJson(Gson gson, Reader reader, TypeToken<T> type, boolean lenient) {
        try {
            JsonReader jsonreader = new JsonReader(reader);
            jsonreader.setLenient(lenient);
            return gson.getAdapter(type).read(jsonreader);
        } catch (IOException ioexception) {
            throw new JsonParseException(ioexception);
        }
    }

    public static <T> T fromJson(Gson gson, Reader reader, TypeToken<T> type, boolean lenient) {
        T t = fromNullableJson(gson, reader, type, lenient);
        if (t == null) {
            throw new JsonParseException("JSON data was null or empty");
        } else {
            return t;
        }
    }

    @Nullable
    public static <T> T fromNullableJson(Gson gson, String json, TypeToken<T> type, boolean lenient) {
        return fromNullableJson(gson, new StringReader(json), type, lenient);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> adapter, boolean lenient) {
        return fromJson(gson, new StringReader(json), adapter, lenient);
    }

    @Nullable
    public static <T> T fromNullableJson(Gson gson, String json, Class<T> adapter, boolean lenient) {
        return fromNullableJson(gson, new StringReader(json), adapter, lenient);
    }

    public static <T> T fromJson(Gson gson, Reader reader, TypeToken<T> type) {
        return fromJson(gson, reader, type, false);
    }

    @Nullable
    public static <T> T fromNullableJson(Gson gson, String json, TypeToken<T> type) {
        return fromNullableJson(gson, json, type, false);
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> jsonClass) {
        return fromJson(gson, reader, jsonClass, false);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> adapter) {
        return fromJson(gson, json, adapter, false);
    }

    public static JsonObject parse(String json, boolean lenient) {
        return parse(new StringReader(json), lenient);
    }

    public static JsonObject parse(Reader reader, boolean lenient) {
        return fromJson(GSON, reader, JsonObject.class, lenient);
    }

    public static JsonObject parse(String json) {
        return parse(json, false);
    }

    public static JsonObject parse(Reader reader) {
        return parse(reader, false);
    }

    public static JsonArray parseArray(String string) {
        return parseArray(new StringReader(string));
    }

    public static JsonArray parseArray(Reader reader) {
        return fromJson(GSON, reader, JsonArray.class, false);
    }

    public static String toStableString(JsonElement json) {
        StringWriter stringwriter = new StringWriter();
        JsonWriter jsonwriter = new JsonWriter(stringwriter);

        try {
            writeValue(jsonwriter, json, Comparator.naturalOrder());
        } catch (IOException ioexception) {
            throw new AssertionError(ioexception);
        }

        return stringwriter.toString();
    }

    public static void writeValue(JsonWriter writer, @Nullable JsonElement jsonElement, @Nullable Comparator<String> sorter) throws IOException {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            writer.nullValue();
        } else if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive jsonprimitive = jsonElement.getAsJsonPrimitive();
            if (jsonprimitive.isNumber()) {
                writer.value(jsonprimitive.getAsNumber());
            } else if (jsonprimitive.isBoolean()) {
                writer.value(jsonprimitive.getAsBoolean());
            } else {
                writer.value(jsonprimitive.getAsString());
            }
        } else if (jsonElement.isJsonArray()) {
            writer.beginArray();

            for (JsonElement jsonelement : jsonElement.getAsJsonArray()) {
                writeValue(writer, jsonelement, sorter);
            }

            writer.endArray();
        } else {
            if (!jsonElement.isJsonObject()) {
                throw new IllegalArgumentException("Couldn't write " + jsonElement.getClass());
            }

            writer.beginObject();

            for (Map.Entry<String, JsonElement> entry : sortByKeyIfNeeded(jsonElement.getAsJsonObject().entrySet(), sorter)) {
                writer.name(entry.getKey());
                writeValue(writer, entry.getValue(), sorter);
            }

            writer.endObject();
        }
    }

    private static Collection<Map.Entry<String, JsonElement>> sortByKeyIfNeeded(
            Collection<Map.Entry<String, JsonElement>> entries, @Nullable Comparator<String> sorter
    ) {
        if (sorter == null) {
            return entries;
        } else {
            List<Map.Entry<String, JsonElement>> list = new ArrayList<>(entries);
            list.sort(Map.Entry.comparingByKey(sorter));
            return list;
        }
    }
}
