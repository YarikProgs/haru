package net.aros.haru.compile;

import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compiler implements Expr.Visitor<Value>, Stmt.Visitor {
    private final Map<String, Value> defaults = new HashMap<>();
    private final Map<String, Value> values = new HashMap<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final DiagnosticCollector diagnosticCollector;
    private final Program program;

    public Compiler(DiagnosticCollector diagnosticCollector, Program program) {
        this.diagnosticCollector = diagnosticCollector;
        this.program = program;
    }

    public Compiler setDefault(String name, Value value) {
        defaults.put(name, value);
        return this;
    }

    public CompiledProgram compile() {
        for (Stmt stmt : program.statements()) {
            visit(stmt);
        }
        return new CompiledProgram(defaults, values, instructions);
    }

    @Override
    public Value visitVariable(Expr.@NotNull Variable expr) {
        return Value.variableValue(() -> values.getOrDefault(expr.name().lexeme(), Value.nullValue()));
    }

    @Override
    public Value visitLiteral(Expr.@NotNull Literal expr) {
        return switch (expr.value()) {
            case Double d -> Value.number(d);
            case String s -> Value.string(s);
            case ParsedText parsedText -> Value.text(parsedText);
            case null -> Value.nullValue();
            default -> throw new IllegalArgumentException("Unknown literal type: " + expr.value().getClass());
        };
    }

    @Override
    public Value visitPrefixedLiteral(Expr.@NotNull PrefixedLiteral expr) {
        return Value.prefixedValue(expr.prefix().lexeme(), visit(expr.value()));
    }

    @Override
    public void visitLabel(Stmt.@NotNull Label stmt) {
        String name = stmt.name().lexeme();
        if (values.containsKey(name)) {
            diagnosticCollector.error("Label '" + name + "' overrides other variable/label '" + name + "'", stmt.pos());
            return;
        }
        values.put(name, Value.number(instructions.size()));
    }

    @Override
    public void visitCommand(Stmt.@NotNull Command stmt) {
        List<Value> arguments = new ArrayList<>();
        for (Expr argument : stmt.arguments()) {
            arguments.add(visit(argument));
        }
        instructions.add(new Instruction.Command(stmt.name().lexeme(), arguments));
    }

    @Override
    public void visitLine(Stmt.@NotNull Line stmt) {
        if (stmt.items().isEmpty()) {
            // How. Why?
            return;
        }
        instructions.add(lineStmtAsInstruction(stmt));
    }

    @Override
    public void visitSystem(Stmt.@NotNull System stmt) {
        if (stmt.lines().isEmpty()) {
            // How? Why?
            return;
        }
        if (stmt.lines().size() == 1) {
            diagnosticCollector.warn("System with 1 line", stmt.pos());
            diagnosticCollector.hint("Consider replacing system with line", stmt.pos());
        }

        List<Instruction.Line> lines = new ArrayList<>();
        for (Stmt.Line line : stmt.lines()) {
            lines.add(lineStmtAsInstruction(line));
        }
        instructions.add(new Instruction.ParallelLines(lines));
    }

    @Override
    public void visitDeclaration(Stmt.@NotNull Declaration stmt) {
        String name = stmt.name().lexeme();
        if (values.containsKey(name)) {
            diagnosticCollector.error("Variable '" + name + "' overrides other variable/label '" + name + "'", stmt.pos());
            return;
        }
        values.put(name, visit(stmt.value()));
    }

    @Contract("_ -> new")
    private Instruction.@NotNull Line lineStmtAsInstruction(Stmt.@NotNull Line lineStmt) {
        List<Value> items = new ArrayList<>();
        for (Expr expr : lineStmt.items()) {
            items.add(visit(expr));
        }
        return new Instruction.Line(items);
    }
}
