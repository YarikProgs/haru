package net.aros.haru.compile;

import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.translation.TranslationFile;
import net.aros.haru.translation.TranslationMerger;
import net.aros.haru.value.HaruFunction;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//TODO: 'end.'
public class Compiler implements Expr.Visitor, Stmt.Visitor {
    private final List<Instruction> instructions = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final Program program;
    private final Map<String, Integer> slots = new HashMap<>();
    private final List<HaruFunction> functions = new ArrayList<>();
    private final Map<String, Integer> functionSlots = new HashMap<>();
    private final TranslationMerger translationMerger;
    private int slot = 0;
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

    public Compiler declareFunction(String name, HaruFunction function) {
        functions.add(function);
        functionSlots.put(name, functions.size() - 1);
        return this;
    }

    public Compiler declareExternalVariable(String name) {
        slots.put(name, slot++);
        return this;
    }

    public Compiler declareExternalVariables(Collection<String> variables) {
        for (String variable : variables) {
            declareExternalVariable(variable);
        }
        return this;
    }

    public CompiledProgram compile() {
        for (Stmt stmt : program.statements()) {
            visit(stmt);
        }
        emit(Instruction.End.INSTANCE);
        if (compilationFailed) return null;
        return new CompiledProgram(
                instructions.toArray(Instruction[]::new),
                functions.toArray(HaruFunction[]::new),
                translationMerger.merge(),
                Map.copyOf(slots)
        );
    }

    /**
     * Index of the phrase: the position of its text in the result.
     * <p>
     * Every text occurrence gets its own index, in source order, so {@code textParts[i]}
     * always corresponds to the i-th entry of the resulting translation file - even if
     * the same phrase is used several times in the source.
     *
     * @return index of the text
     */
    private int addTextPart(@NotNull ParsedText text) {
        translationMerger.addText(text);
        return textCount++;
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
            case Double d -> Value.number(d);
            case String s -> Value.string(s);
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
    public void visitCall(Expr.@NotNull Call expr) {
        for (Expr arg : expr.args()) {
            visit(arg);
        }
        Token callee = expr.callee();
        if (!functionSlots.containsKey(callee.lexeme())) {
            diagnosticCollector.error("Unknown function: '" + callee.lexeme() + "'", callee.pos());
            compilationFailed = true;
            return;
        }
        emit(new Instruction.Call(functionSlots.get(callee.lexeme()), expr.args().size()));
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
            diagnosticCollector.hint("Consider replacing system with line (remove the '{')", stmt.pos());
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
        int slot = this.slot++;
        slots.put(name, slot);
        visit(stmt.value());
        emit(new Instruction.Store(slot));
    }

    private void emit(Instruction instruction) {
        instructions.add(instruction);
    }
}
