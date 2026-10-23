package net.aros.haru.compile.io;

import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ValueIO {
    public static Value readValue(@NotNull DataInputStream dis) throws IOException {
        Value.Tag tag = Value.Tag.values()[dis.readByte()];
        return switch (tag) {
            case NUMBER -> Value.number(dis.readDouble());
            case BOOL -> Value.bool(dis.readBoolean());
            case LITERAL_REF -> Value.literalRef(dis.readInt());
            case TEXT_REF -> Value.textRef(dis.readInt());
            case NULL -> Value.nullValue();
        };
    }

    public static void writeValue(DataOutputStream dos, @NotNull Value v) throws IOException {
        if (v.isNumber()) {
            dos.writeByte(Value.Tag.NUMBER.id());
            dos.writeDouble(v.asNumber());
        }
        else if (v.isBool()) {
            dos.writeByte(Value.Tag.BOOL.id());
            dos.writeBoolean(v.asBool());
        }
        else if (v.isLiteralRef()) {
            dos.writeByte(Value.Tag.LITERAL_REF.id());
            dos.writeInt(v.asLiteralRef());
        }
        else if (v.isTextRef()) {
            dos.writeByte(Value.Tag.TEXT_REF.id());
            dos.writeInt(v.asTextRef());
        }
        else if (v.isNull()) {
            dos.writeByte(Value.Tag.NULL.id());
        }
        else {
            throw new IOException("Unknown value type: " + v);
        }
    }
}
