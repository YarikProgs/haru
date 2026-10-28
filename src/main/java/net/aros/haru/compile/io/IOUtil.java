package net.aros.haru.compile.io;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class IOUtil {
    public static final int MAX_ARRAY_LENGTH = 4096;
    public static final String HASH_ALGO = "SHA-256";
    public static final int HASH_SIZE_BYTES = 32;

    public static byte @NotNull [] hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] result = digest.digest();
            if (result.length == HASH_SIZE_BYTES)
                return result;
            return Arrays.copyOf(result, HASH_SIZE_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static int @NotNull [] readIntArray(DataInputStream dis) throws IOException {
        int[] arr = new int[readLen(dis)];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dis.readInt();
        }
        return arr;
    }

    public static int readLen(DataInputStream dis) throws IOException {
        int n = dis.readInt();
        if (n < 0 || n > MAX_ARRAY_LENGTH)
            throw new IOException("Corrupt length: " + n);
        return n;
    }

    public static void writeString(DataOutputStream dos, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    @Contract("_ -> new")
    public static @NotNull String readString(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[readLen(dis)];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
