package net.aros.haru.compile;

import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.value.HaruFunction;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: 'end.'
public class Compiler implements Expr.Visitor<Void>, Stmt.Visitor {
    private final List<Instruction> instructions = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final Program program;
    private final Map<String, Integer> slots = new HashMap<>();
    private final Map<String, HaruFunction> functions = new HashMap<>();
    private int slot = 0;
    private boolean compilationFailed = false;

    public Compiler(DiagnosticCollector diagnosticCollector, Program program) {
        this.diagnosticCollector = diagnosticCollector;
        this.program = program;
    }

    public Compiler declareFunction(String name, HaruFunction function) {
        functions.put(name, function);
        return this;
    }

    public Compiler declareExternalVariable(String name) {
        slots.put(name, slot++);
        return this;
    }

    public CompiledProgram compile() {
        for (Stmt stmt : program.statements()) {
            visit(stmt);
        }
        emit(Instruction.End.INSTANCE);
        if (compilationFailed) return null;
        return new CompiledProgram(instructions.toArray(Instruction[]::new), Map.copyOf(slots));
    }

    @Override
    public Void visitVariable(Expr.@NotNull Variable expr) {
        Integer slot = slots.get(expr.name().lexeme());
        if (slot == null) {
            diagnosticCollector.error("Unknown variable: '" + expr.name().lexeme() + "'", expr.name().pos());
            compilationFailed = true;
            return null;
        }
        emit(new Instruction.Load(slot));
        return null;
    }

    @Override
    public Void visitLiteral(Expr.@NotNull Literal expr) {
        Value value = switch (expr.value()) {
            case Double d -> Value.number(d);
            case String s -> Value.string(s);
            case ParsedText parsedText -> Value.text(parsedText);
            case null -> Value.nullValue();
            default -> throw new IllegalArgumentException("Unknown literal type: " + expr.value().getClass());
        };
        emit(new Instruction.PushConst(value));
        return null;
    }

    @Override
    public void visitExprStmt(Stmt.@NotNull ExprStmt stmt) {
        visit(stmt.expr());
        emit(Instruction.Pop.INSTANCE);
    }

    @Override
    public void visitEnd(Stmt.End stmt) {
        emit(Instruction.End.INSTANCE);
    }

    @Override
    public Void visitCall(Expr.@NotNull Call expr) {
        for (Expr arg : expr.args()) {
            visit(arg);
        }
        Token callee = expr.callee();
        if (!functions.containsKey(callee.lexeme())) {
            diagnosticCollector.error("Unknown function: '" + callee.lexeme() + "'", callee.pos());
            compilationFailed = true;
            return null;
        }
        emit(new Instruction.Call(functions.get(callee.lexeme()), expr.args().size()));
        return null;
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
