package net.aros.haru.ast;

import net.aros.haru.diagnostic.SourcePos;
import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statements
 */
public sealed interface Stmt {
    SourceSpan span();

    /**
     * A statement that represents a line
     * <p>
     * Example: {@code knight, sad, "My sword..."}
     *
     * @param items items inside the line
     */
    record Line(List<Expr> items) implements Stmt {
        @Contract(" -> new")
        @Override
        public @NotNull SourceSpan span() {
            if (items.isEmpty()) throw new IllegalArgumentException("Illegal line with no items");
            return new SourceSpan(items.getFirst().span().start(), items.getLast().span().end());
        }
    }

    /**
     * A statement that represents a system
     *
     * @param lines lines that will be shown parallel
     */
    record System(Token parallel, List<Line> lines, Token rbrace) implements Stmt {
        @Override
        public SourceSpan span() {
            if (lines.isEmpty()) throw new IllegalArgumentException("System with no lines");
            return new SourceSpan(parallel.span().start(), rbrace.span().end());
        }
    }

    /**
     * A statement that represents a declaration
     * <p>
     * Examples: {@code let a = 20}, {@code let importance = 100}
     *
     * @param name  name of the variable
     * @param value constant of the variable
     */
    record Declaration(Token let, Token name, Expr value) implements Stmt {
        @Contract(" -> new")
        @Override
        public @NotNull SourceSpan span() {
            return new SourceSpan(let.span().start(), value.span().end());
        }
    }

    /**
     * A statement that represents an expression
     * <p>
     * Examples: {@code 3}, {@code wait(2)}
     *
     * @param expr expression
     */
    record ExprStmt(Expr expr) implements Stmt {
        @Override
        public SourceSpan span() {
            return expr.span();
        }
    }

    /**
     * A statement that represents the end of dialogue (terminator)
     * <p>
     * Examples: {@code end.}
     *
     * @param token 'end.' token
     */
    record End(Token token) implements Stmt {
        @Override
        public SourceSpan span() {
            return token.span();
        }
    }

    record NamedBlock(Token name, Block block) implements Stmt {
        @Contract(" -> new")
        @Override
        public @NotNull SourceSpan span() {
            return new SourceSpan(name.span().start(), block.span().end());
        }
    }

    record Do(Token namedBlock) implements Stmt {
        @Override
        public SourceSpan span() {
            return namedBlock.span();
        }
    }

    record If(Token ifToken, Expr cond, Block then, @Nullable Block orElse) implements Stmt {
        @Override
        public @NotNull SourceSpan span() {
            SourcePos end = orElse != null ? orElse.span().end() : then.span().end();
            return new SourceSpan(ifToken.span().start(), end);
        }
    }

    record Block(Token lbrace, List<Stmt> stmts, Token rbrace) {
        @Contract(" -> new")
        public @NotNull SourceSpan span() {
            if (lbrace == null || rbrace == null) {
                if (stmts.isEmpty()) throw new IllegalStateException("Synthetic block with no statement");
                return stmts.getFirst().span();
            }

            return new SourceSpan(lbrace.span().start(), rbrace.span().end());
        }
    }

    interface Visitor {
        void visitLine(Line stmt);

        void visitSystem(System stmt);

        void visitDeclaration(Declaration stmt);

        void visitExprStmt(ExprStmt stmt);

        void visitEnd(End stmt);

        void visitNamedBlock(NamedBlock stmt);

        void visitDo(Do stmt);

        void visitIf(If stmt);

        default void visit(@NotNull Stmt stmt) {
            switch (stmt) {
                case Line line -> visitLine(line);
                case System system -> visitSystem(system);
                case Declaration decl -> visitDeclaration(decl);
                case ExprStmt exprStmt -> visitExprStmt(exprStmt);
                case End end -> visitEnd(end);
                case NamedBlock nb -> visitNamedBlock(nb);
                case Do doStmt -> visitDo(doStmt);
                case If ifStmt -> visitIf(ifStmt);
            }
        }
    }
}
