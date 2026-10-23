package net.aros.haru.compile;

import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.translation.TranslationFile;
import net.aros.haru.translation.TranslationMerger;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Compiler implements Expr.Visitor, Stmt.Visitor {
    private final List<Instruction> instructions = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final Program program;
    private final Map<String, Integer> slots = new HashMap<>();
    private final Map<String, Integer> functionSlots = new HashMap<>();
    private final List<String> literals = new ArrayList<>();
    private final List<Stmt.NamedBlock> pendingBlocks = new ArrayList<>();
    private final Map<String, Integer> blockAddresses = new HashMap<>();
    private final List<DoSite> doSites = new ArrayList<>();
    private final TranslationMerger translationMerger;
    private int textCount = 0;
    private boolean compilationFailed = false;

    public Compiler(DiagnosticCollector diagnosticCollector, Program program, @Nullable TranslationFile oldTranslationFile) {
        this.diagnosticCollector = diagnosticCollector;
        this.program = program;
        this.translationMerger = new TranslationMerger(oldTranslationFile);
    }

    /**
     * Locales that must be present in the 'translations' map of the resulting translation file.
     * Missing locales are filled with a copy of the original and marked fuzzy.
     */
    public Compiler withLocales(@NotNull Collection<String> locales) {
        translationMerger.withLocales(locales);
        return this;
    }

    public Compiler declareExternal(String name) {
        slots.put(name, slots.size());
        return this;
    }

    public Compiler declareExternalAll(@NotNull Collection<String> variables) {
        for (String variable : variables) {
            declareExternal(variable);
        }
        return this;
    }

    public CompiledProgram compile() {
        for (Stmt stmt : program.statements()) {
            visit(stmt);
        }
        emit(Instruction.End.INSTANCE);
        patchBlocksAndDos();
        if (compilationFailed) return null;
        return new CompiledProgram(
                instructions.toArray(Instruction[]::new),
                translationMerger.merge(),
                literals.toArray(String[]::new),
                Map.copyOf(slots),
                Map.copyOf(functionSlots)
        );
    }

    private void patchBlocksAndDos() {
        for (Stmt.NamedBlock pendingBlock : pendingBlocks) {
            blockAddresses.put(pendingBlock.name().lexeme(), instructions.size());
            compileBlock(pendingBlock.block());
            emit(Instruction.Return.INSTANCE);
        }
        for (DoSite site : doSites) {
            Integer target = blockAddresses.get(site.name().lexeme());
            if (target == null) {
                diagnosticCollector.error("Unknown block: '" + site.name().lexeme() + "'", site.name().pos());
                compilationFailed = true;
                continue;
            }
            instructions.set(site.index(), new Instruction.DoBlock(target));
        }
    }

    private int addTextPart(@NotNull ParsedText text) {
        translationMerger.addText(text);
        return textCount++;
    }

    private int addLiteral(String s) {
        if (literals.contains(s))
            return literals.indexOf(s);
        int index = literals.size();
        literals.add(s);
        return index;
    }

    @Override
    public void visitVariable(Expr.@NotNull Variable expr) {
        Integer slot = slots.get(expr.name().lexeme());
        if (slot == null) {
            diagnosticCollector.error("Unknown variable: '" + expr.name().lexeme() + "'", expr.name().pos());
            compilationFailed = true;
            return;
        }
        emit(new Instruction.Load(slot));
    }

    @Override
    public void visitLiteral(Expr.@NotNull Literal expr) {
        Value value = switch (expr.value()) {
            case Boolean b -> Value.bool(b);
            case Double d -> Value.number(d);
            case String s -> Value.literalRef(addLiteral(s));
            case ParsedText text -> Value.textRef(addTextPart(text));
            case null -> Value.nullValue();
            default -> throw new IllegalArgumentException("Unknown literal type: " + expr.value().getClass());
        };
        emit(new Instruction.PushConst(value));
    }

    @Override
    public void visitExprStmt(Stmt.@NotNull ExprStmt stmt) {
        if (stmt.expr() instanceof Expr.Literal literal) {
            diagnosticCollector.warn(literal.token().lexeme() + " doesn't do anything", stmt.pos());
            diagnosticCollector.hint("If you wanted to create a line, add ',' to the end", stmt.pos());
        }
        visit(stmt.expr());
        emit(Instruction.Pop.INSTANCE);
    }

    @Override
    public void visitEnd(Stmt.End stmt) {
        emit(Instruction.End.INSTANCE);
    }

    @Override
    public void visitNamedBlock(Stmt.@NotNull NamedBlock stmt) {
        String name = stmt.name().lexeme();
        if (pendingBlocks.stream().anyMatch(b -> b.name().lexeme().equals(name))) {
            diagnosticCollector.error("Block '" + name + "' already defined", stmt.name().pos());
            compilationFailed = true;
            return;
        }
        pendingBlocks.add(stmt);
    }

    @Override
    public void visitDo(Stmt.@NotNull Do stmt) {
        emit(new Instruction.DoBlock(-1));
        doSites.add(new DoSite(instructions.size() - 1, stmt.namedBlock()));
    }

    @Override
    public void visitCall(Expr.@NotNull Call expr) {
        for (Expr arg : expr.args()) {
            visit(arg);
        }
        Token callee = expr.callee();
        int slot = functionSlots.computeIfAbsent(callee.lexeme(), ignored -> functionSlots.size());
        emit(new Instruction.Call(slot, expr.args().size()));
    }

    @Override
    public void visitLine(Stmt.@NotNull Line stmt) {
        for (Expr expr : stmt.items()) {
            visit(expr);
        }
        emit(new Instruction.Line(stmt.items().size()));
    }

    @Override
    public void visitSystem(Stmt.@NotNull System stmt) {
        if (stmt.lines().size() == 1) {
            diagnosticCollector.warn("System with single line", stmt.pos());
            diagnosticCollector.hint("Consider replacing system with line (remove 'parallel' and block)", stmt.pos());
        }

        int[] argcs = new int[stmt.lines().size()];
        int i = 0;
        for (Stmt.Line line : stmt.lines()) {
            argcs[i++] = line.items().size();
            for (Expr item : line.items()) visit(item);
        }
        emit(new Instruction.ParallelLines(argcs));
    }

    @Override
    public void visitDeclaration(Stmt.@NotNull Declaration stmt) {
        String name = stmt.name().lexeme();
        if (slots.containsKey(name)) {
            diagnosticCollector.error("Variable '" + name + "' already defined", stmt.name().pos());
            compilationFailed = true;
            return;
        }
        int slot = slots.computeIfAbsent(name, ignored -> slots.size());
        visit(stmt.value());
        emit(new Instruction.Store(slot));
    }

    @Override
    public void visitIf(Stmt.@NotNull If stmt) {
        visit(stmt.cond());
        int jumpToElse = emitJump(new Instruction.JumpIfFalse(-1));
        compileBlock(stmt.then());

        if (stmt.orElse() == null) {
            patchJump(jumpToElse);
        } else {
            int jumpToEnd = emitJump(new Instruction.Jump(-1));
            patchJump(jumpToElse);
            compileBlock(stmt.orElse());
            patchJump(jumpToEnd);
        }
    }

    private void compileBlock(Stmt.@NotNull Block block) {
        for (Stmt stmt : block.stmts()) {
            visit(stmt);
        }
    }

    private int emitJump(Instruction jump) {
        emit(jump);
        return instructions.size() - 1;
    }

    private void patchJump(int index) {
        int target = instructions.size();
        instructions.set(index, switch (instructions.get(index)) {
            case Instruction.Jump ignored -> new Instruction.Jump(target);
            case Instruction.JumpIfFalse ignored -> new Instruction.JumpIfFalse(target);
            default -> throw new IllegalStateException("Not a jump at " + index);
        });
    }

    private void emit(Instruction instruction) {
        instructions.add(instruction);
    }

    private record DoSite(int index, Token name) {}
}
