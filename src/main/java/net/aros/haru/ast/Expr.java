package net.aros.haru.ast;

import net.aros.haru.lex.SourcePos;
import net.aros.haru.lex.Token;
import org.jetbrains.annotations.NotNull;

/**
 * Expressions
 * */
public sealed interface Expr {
    /**
     * @return position of expression in source file
     */
    SourcePos pos();

    /**
     * An expression that represents a variable
     * <p>
     * Example: {@code a}
     *
     * @param name name of the variable
     */
    record Variable(Token name) implements Expr {
        @Override
        public SourcePos pos() {
            return name.pos();
        }
    }

    /**
     * An expression that represents a literal value
     * <p>
     * Examples: {@code 10}, {@code "Hello world!"}, {@code null}
     *
     * @param value literal
     */
    record Literal(Object value, SourcePos pos) implements Expr {
    }

    /**
     * An expression that represents a literal value but with a string prefix
     * <p>
     * Examples: {@code proc "aengine:choose_line"}, {@code char "aengine:aros"}
     *
     * @param prefix prefix
     * @param value literal
     */
    record PrefixedLiteral(Token prefix, Literal value) implements Expr {
        @Override
        public SourcePos pos() {
            return prefix.pos();
        }
    }

    interface Visitor<T> {
        T visitVariable(Variable expr);

        T visitLiteral(Literal expr);

        T visitPrefixedLiteral(PrefixedLiteral expr);

        default T visit(@NotNull Expr expr) {
            return switch (expr) {
                case Variable variable -> visitVariable(variable);
                case Literal literal -> visitLiteral(literal);
                case PrefixedLiteral prefixedLiteral -> visitPrefixedLiteral(prefixedLiteral);
            };
        }
    }
}
