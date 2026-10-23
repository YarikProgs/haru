package net.aros.haru.ast;

import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statements
 */
public sealed interface Stmt {
    SourcePos pos();

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
     * @param value constant of the variable
     */
    record Declaration(Token name, Expr value) implements Stmt {
        @Override
        public SourcePos pos() {
            return name.pos();
        }
    }

    /**
     * A statement that represents an expression
     * <p>
     * Examples: {@code 3}, {@code wait(2)}
     * @param expr expression
     */
    record ExprStmt(Expr expr) implements Stmt {
        @Override
        public SourcePos pos() {
            return expr.pos();
        }
    }

    /**
     * A statement that represents the end of dialogue (terminator)
     * <p>
     * Examples: {@code end.}
     * @param token 'end.' token
     */
    record End(Token token) implements Stmt {
        @Override
        public SourcePos pos() {
            return token.pos();
        }
    }

    record If(Token ifToken, Expr cond, Block then, @Nullable Block orElse) implements Stmt {
        @Override
        public SourcePos pos() {
            return ifToken.pos();
        }
    }

    record Block(List<Stmt> stmts) {
    }

    interface Visitor {
        void visitLine(Line stmt);

        void visitSystem(System stmt);

        void visitDeclaration(Declaration stmt);

        void visitExprStmt(ExprStmt stmt);

        void visitEnd(End stmt);

        void visitIf(If stmt);

        default void visit(@NotNull Stmt stmt) {
            switch (stmt) {
                case Line line -> visitLine(line);
                case System system -> visitSystem(system);
                case Declaration decl -> visitDeclaration(decl);
                case ExprStmt exprStmt -> visitExprStmt(exprStmt);
                case End end -> visitEnd(end);
                case If ifStmt -> visitIf(ifStmt);
            }
        }
    }
}
