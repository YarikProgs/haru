package net.aros.haru.translation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import java.io.*;

public class TranslationFileIO {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static DataResult<TranslationFile> importFile(InputStream inputStream) {
        JsonElement element = GSON.fromJson(new InputStreamReader(inputStream), JsonElement.class);
        if (element == null) return DataResult.error(() -> "Failed to parse the file");
        return TranslationFile.CODEC.parse(JsonOps.INSTANCE, element);
    }

    public static void exportFile(TranslationFile file, OutputStream outputStream) throws IOException {
        JsonElement result = TranslationFile.CODEC.encodeStart(JsonOps.INSTANCE, file).getOrThrow();
//        GSON.toJson(result, new OutputStreamWriter(outputStream));
        String text = GSON.toJson(result);
        var writer = new OutputStreamWriter(outputStream);
        writer.write(text);
        writer.flush();
    }
}
