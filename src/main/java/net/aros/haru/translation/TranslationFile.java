package net.aros.haru.translation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record TranslationFile(List<TextEntry> entries, List<TextEntry> removed) {
    public static final Codec<TranslationFile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            TextEntry.CODEC.listOf().fieldOf("entries").forGetter(TranslationFile::entries),
            TextEntry.CODEC.listOf().fieldOf("removed").forGetter(TranslationFile::removed)
    ).apply(inst, TranslationFile::new));
}
