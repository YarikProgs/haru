package net.aros.haru.ast;

import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Statements
 */
public sealed interface Stmt {
    SourcePos pos();

    /**
     * A statement that represents a label
     * <p>
     * Examples: {@code label:}, {@code after_fight:}
     *
     * @param name name of the label
     */
    record Label(Token name) implements Stmt {
        @Override
        public SourcePos pos() {
            return name.pos();
        }
    }

    /**
     * A statement that represents a command
     * <p>
     * Examples: {@code goto label}, {@code wait 50}
     *
     * @param name      name of the command
     * @param arguments arguments passed to the command
     */
    record Command(Token name, List<Expr> arguments) implements Stmt {
        @Override
        public SourcePos pos() {
            return name.pos();
        }
    }

    /**
     * A statement that represents a line
     * <p>
     * Example: {@code knight, sad, "My sword..."}
     *
     * @param items items inside the line
     */
    record Line(List<Expr> items) implements Stmt {
        @Override
        public SourcePos pos() {
            if (items.isEmpty()) throw new IllegalArgumentException("Illegal line with no items");
            return items.getFirst().pos();
        }
    }

    /**
     * A statement that represents a system
     *
     * @param lines lines that will be shown parallel
     */
    record System(List<Line> lines) implements Stmt {
        @Override
        public SourcePos pos() {
            if (lines.isEmpty()) throw new IllegalArgumentException("Illegal system with no lines");
            return lines.getFirst().pos();
        }
    }

    /**
     * A statement that represents a declaration
     * <p>
     * Examples: {@code a = 20}, {@code importance = 100}
     *
     * @param name  name of the variable
     * @param value value of the variable
     */
    record Declaration(Token name, Expr value) implements Stmt {
        @Override
        public SourcePos pos() {
            return name.pos();
        }
    }

    interface Visitor {
        void visitLabel(Label stmt);

        void visitCommand(Command stmt);

        void visitLine(Line stmt);

        void visitSystem(System stmt);

        void visitDeclaration(Declaration stmt);

        default void visit(@NotNull Stmt stmt) {
            switch (stmt) {
                case Label label -> visitLabel(label);
                case Command command -> visitCommand(command);
                case Line line -> visitLine(line);
                case System system -> visitSystem(system);
                case Declaration decl -> visitDeclaration(decl);
            }
        }
    }
}
