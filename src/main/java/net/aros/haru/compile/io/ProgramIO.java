package net.aros.haru.compile.io;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Instruction;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.translation.TranslationFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ProgramIO {
    private static final byte[] MAGIC = {(byte) 0x89, 'H', 'A', 'R', 'U', 0x0D, 0x0A, 0x1A};
    private static final short FORMAT_VERSION = 0;

    public static void exportFile(String source, CompiledProgram program, OutputStream stream) throws IOException {
        DataOutputStream dos = new DataOutputStream(stream);

        dos.write(MAGIC);
        dos.writeShort(FORMAT_VERSION);
        dos.write(IOUtil.hash(source));
        writeLiterals(dos, program.literals());
        writeSlots(dos, program.slots());
        writeSlots(dos, program.functionSlots());
        InstructionIO.writeInstructions(dos, program.instructions());
    }

    /**
     * <h1>
     * !!! Expects magic to be read by InputStream
     * </h1><p>
     * Call {@link #isFreshHeader} before
     */
    @Contract("_, _ -> new")
    public static @NotNull CompiledProgram importFile(TranslationFile translationFile, InputStream stream) throws IOException {
        DataInputStream dis = new DataInputStream(stream);

        String[] literals = readLiterals(dis);
        Map<String, Integer> slots = readSlots(dis);
        Map<String, Integer> functionSlots = readSlots(dis);
        Instruction[] instructions = InstructionIO.readInstructions(dis);

        return new CompiledProgram(instructions, translationFile, literals, slots, functionSlots);
    }

    public static boolean isFreshHeader(String source, InputStream stream, DiagnosticCollector diagnosticCollector) throws IOException {
        DataInputStream dis = new DataInputStream(stream);
        if (!validateMagic(dis, diagnosticCollector) || dis.readShort() != FORMAT_VERSION) {
            return false;
        }
        byte[] hash = new byte[IOUtil.HASH_SIZE_BYTES];
        try {
            dis.readFully(hash);
        } catch (EOFException e) {
            return false;
        }
        return Arrays.equals(IOUtil.hash(source), hash);
    }

    private static boolean validateMagic(DataInputStream dis, DiagnosticCollector diagnosticCollector) throws IOException {
        byte[] buf = new byte[MAGIC.length];
        dis.readFully(buf);
        if (!Arrays.equals(buf, MAGIC)) {
            diagnosticCollector.error("Not a .haruc file", null);
            return false;
        }
        return true;
    }

    private static void writeLiterals(DataOutputStream dos, String[] literals) throws IOException {
        dos.writeInt(literals.length);
        for (String literal : literals) {
            IOUtil.writeString(dos, literal);
        }
    }

    private static String @NotNull [] readLiterals(DataInputStream dis) throws IOException {
        String[] literals = new String[IOUtil.readLen(dis)];
        for (int i = 0; i < literals.length; i++) {
            literals[i] = IOUtil.readString(dis);
        }
        return literals;
    }

    private static void writeSlots(DataOutputStream dos, Map<String, Integer> slots) throws IOException {
        dos.writeInt(slots.size());
        for (Map.Entry<String, Integer> entry : slots.entrySet()) {
            IOUtil.writeString(dos, entry.getKey());
            dos.writeInt(entry.getValue());
        }
    }

    private static @NotNull Map<String, Integer> readSlots(DataInputStream dis) throws IOException {
        int cap = dis.readInt();
        Map<String, Integer> slots = new HashMap<>(cap);
        for (int i = 0; i < cap; i++) {
            slots.put(IOUtil.readString(dis), dis.readInt());
        }
        return slots;
    }
}
