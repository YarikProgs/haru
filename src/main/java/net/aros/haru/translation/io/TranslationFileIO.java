package net.aros.haru.translation.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.aros.haru.translation.TranslationFile;

import java.io.*;
import java.util.Optional;

public class TranslationFileIO {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static Optional<TranslationFile> importFile(InputStream inputStream) {
        try {
            return Optional.ofNullable(GSON.fromJson(new InputStreamReader(inputStream), TranslationFile.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void exportFile(TranslationFile file, OutputStream outputStream) throws IOException {
        String text = GSON.toJson(file);
        var writer = new OutputStreamWriter(outputStream);
        writer.write(text);
        writer.flush();
    }
}
