package net.aros.haru.ast;

import net.aros.haru.diagnostic.SourceSpan;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Expressions
 * */
public sealed interface Expr {
    /**
     * @return start+end positions of the expression in source file
     */
    SourceSpan span();

    /**
     * @return does expression have side effects (true if not)
     */
    boolean isPure();

    /**
     * An expression that represents a variable
     * <p>
     * Example: {@code a}
     *
     * @param name name of the variable
     */
    record Variable(Token name) implements Expr {
        @Override
        public SourceSpan span() {
            return name.span();
        }

        @Override
        public boolean isPure() {
            return true;
        }
    }

    /**
     * An expression that represents a literal constant
     * <p>
     * Examples: {@code 10}, {@code "Hello world!"}, {@code null}
     *
     * @param value literal
     */
    record Literal(Token token, Object value) implements Expr {
        @Override
        public SourceSpan span() {
            return token.span();
        }

        @Override
        public boolean isPure() {
            return true;
        }
    }

    /**
     * An expression that represents a function call
     * <p>
     * Examples: {@code proc("aengine:choose_line")}, {@code char("aengine:aros")}
     * @param callee
     * @param args
     */
    record Call(Token callee, List<Expr> args, Token rparen) implements Expr {
        @Override
        public SourceSpan span() {
            return new SourceSpan(callee.span().start(), rparen.span().end());
        }

        @Override
        public boolean isPure() {
            return false;
        }
    }

    interface Visitor {
        void visitVariable(Variable expr);

        void visitLiteral(Literal expr);

        void visitCall(Call expr);

        default void visit(@NotNull Expr expr) {
            switch (expr) {
                case Variable variable -> visitVariable(variable);
                case Literal literal -> visitLiteral(literal);
                case Call call -> visitCall(call);
            }
        }
    }
}
