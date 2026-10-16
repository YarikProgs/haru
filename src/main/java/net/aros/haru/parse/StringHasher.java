package net.aros.haru.parse;

import java.nio.charset.StandardCharsets;

public class StringHasher {
    private static final long FNV_64_INIT = 0xcbf29ce484222325L;
    private static final long FNV_64_PRIME = 0x100000001b3L;

    public static long hash(String text) {
        if (text == null) {
            return 0L;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        long hash = FNV_64_INIT;

        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= FNV_64_PRIME;
        }

        return hash;
    }
}
