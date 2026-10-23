package net.aros.haru.compile.io;

import net.aros.haru.compile.Instruction;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InstructionIO {
    public static void writeInstructions(DataOutputStream dos, Instruction[] instructions) throws IOException {
        dos.writeInt(instructions.length);
        for (Instruction instruction : instructions) {
            writeInstruction(dos, instruction);
        }
    }

    public static Instruction @NotNull [] readInstructions(DataInputStream dis) throws IOException {
        Instruction[] instructions = new Instruction[IOUtil.readLen(dis)];
        for (int i = 0; i < instructions.length; i++) {
            instructions[i] = readInstruction(dis);
        }
        return instructions;
    }

    private static void writeInstruction(DataOutputStream dos, @NotNull Instruction instruction) throws IOException {
        switch (instruction) {
            case Instruction.Line(int argc) -> {
                dos.writeByte(Instruction.Opcode.LINE.id());
                dos.writeInt(argc);
            }
            case Instruction.ParallelLines(int[] argcs) -> {
                dos.writeByte(Instruction.Opcode.PARALLEL_LINES.id());
                dos.writeInt(argcs.length);
                for (int argc : argcs) dos.writeInt(argc);
            }
            case Instruction.PushConst(Value constant) -> {
                dos.writeByte(Instruction.Opcode.PUSH_CONST.id());
                ValueIO.writeValue(dos, constant);
            }
            case Instruction.Call(int fn, int argc) -> {
                dos.writeByte(Instruction.Opcode.CALL.id());
                dos.writeInt(fn);
                dos.writeInt(argc);
            }
            case Instruction.Pop() -> dos.writeByte(Instruction.Opcode.POP.id());
            case Instruction.Load(int slot) -> {
                dos.writeByte(Instruction.Opcode.LOAD.id());
                dos.writeInt(slot);
            }
            case Instruction.Store(int slot) -> {
                dos.writeByte(Instruction.Opcode.STORE.id());
                dos.writeInt(slot);
            }
            case Instruction.Jump(int target) -> {
                dos.writeByte(Instruction.Opcode.JUMP.id());
                dos.writeInt(target);
            }
            case Instruction.JumpIfFalse(int target) -> {
                dos.writeByte(Instruction.Opcode.JUMP_IF_FALSE.id());
                dos.writeInt(target);
            }
            case Instruction.End() -> dos.writeByte(Instruction.Opcode.END.id());
            case Instruction.DoBlock(int target) -> {
                dos.writeByte(Instruction.Opcode.DO_BLOCK.id());
                dos.writeInt(target);
            }
            case Instruction.Return() -> dos.writeByte(Instruction.Opcode.RETURN.id());
        }
    }

    private static Instruction readInstruction(@NotNull DataInputStream dis) throws IOException {
        Instruction.Opcode tag = Instruction.Opcode.values()[dis.readByte()];
        return switch (tag) {
            case LINE -> new Instruction.Line(dis.readInt());
            case PARALLEL_LINES -> new Instruction.ParallelLines(IOUtil.readIntArray(dis));
            case PUSH_CONST -> new Instruction.PushConst(ValueIO.readValue(dis));
            case CALL -> new Instruction.Call(dis.readInt(), dis.readInt());
            case POP -> Instruction.Pop.INSTANCE;
            case LOAD -> new Instruction.Load(dis.readInt());
            case STORE -> new Instruction.Store(dis.readInt());
            case JUMP -> new Instruction.Jump(dis.readInt());
            case JUMP_IF_FALSE -> new Instruction.JumpIfFalse(dis.readInt());
            case END -> Instruction.End.INSTANCE;
            case DO_BLOCK -> new Instruction.DoBlock(dis.readInt());
            case RETURN -> Instruction.Return.INSTANCE;
        };
    }
}
