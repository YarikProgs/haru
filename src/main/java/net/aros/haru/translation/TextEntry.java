package net.aros.haru.translation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record TextEntry(String original, Map<String, String> translations, List<String> fuzzy) {
    public static final Codec<TextEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("original").forGetter(TextEntry::original),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("translations").forGetter(TextEntry::translations),
            Codec.STRING.listOf().fieldOf("fuzzy").forGetter(TextEntry::fuzzy)
    ).apply(inst, TextEntry::new));
}
