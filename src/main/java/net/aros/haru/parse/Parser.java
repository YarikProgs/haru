package net.aros.haru.parse;

import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.aros.haru.lex.TokenType.*;

public class Parser {
    private final DiagnosticCollector diagnosticCollector;
    private final List<Token> tokens;
    private int current;

    public Parser(DiagnosticCollector diagnosticCollector, List<Token> tokens) {
        this.diagnosticCollector = diagnosticCollector;
        this.tokens = tokens;
    }

    @Contract(" -> new")
    @NotNull
    public Program parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(statement());
            } catch (ParseError ignored) {
                synchronize();
            }
        }
        return new Program(statements);
    }

    private Stmt statement() {
        if (match(LBRACE)) return system();
        if (check(IDENTIFIER)) {
            Token ident = advance();
            if (match(EQUAL)) return declaration(ident);
            if (match(COLON)) return new Stmt.Label(ident);
            if (check(COMMA)) return line(new Expr.Variable(ident));
            return command(ident);
        }
        return line(expression());
    }

    private Stmt declaration(Token name) {
        var expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Declaration(name, expr);
    }

    @Contract(" -> new")
    private @NotNull Stmt system() {
        List<Stmt.Line> lines = new ArrayList<>();
        do {
            Expr expr = expression();
            consume(COMMA, "Expect ',' after first item in line");
            lines.add(line(expr));
        } while (match(LBRACE));
        return new Stmt.System(lines);
    }

    @Contract("_ -> new")
    private Stmt.@NotNull Line line(Expr first) {
        List<Expr> items = new ArrayList<>();
        items.add(first);
        while (match(COMMA)) {
            if (check(SEMICOLON)) break;
            items.add(expression());
        }
//        consume(COMMA, "Expect ',' after first element of a line");
//        if (!check(SEMICOLON)) {
//            do items.add(expression()); while (match(COMMA));
//        }
        consume(SEMICOLON, "Expect ';' after last element of a line");
        return new Stmt.Line(items);
    }

    @Contract(pure = true)
    private Stmt.@NotNull Command command(Token name) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(SEMICOLON)) {
            do arguments.add(expression()); while (match(COMMA));
        }
        consume(SEMICOLON, "Expect ';' after last argument of a command");
        return new Stmt.Command(name, arguments);
    }

    @Contract(" -> new")
    private @NotNull Expr expression() {
        if (match(IDENTIFIER)) {
            Token identifier = previous();

            Expr.Literal literal = literal();
            if (literal != null) return new Expr.PrefixedLiteral(identifier, literal);

            return new Expr.Variable(identifier);
        }

        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "Expect ')' after expression");
            return expr;
        }

        Expr.Literal literal = literal();
        if (literal == null) throw error(peek(), "Expect expression");
        return literal;
    }

    @Nullable
    private Expr.Literal literal() {
        if (match(NULL)) return new Expr.Literal(null, previous().pos());
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal(), previous().pos());
        if (match(TEXT)) return new Expr.Literal(new TextParser(diagnosticCollector, previous()).parse(), previous().pos());
        return null;
    }

    private void consume(TokenType type, String message) {
        if (!check(type)) throw error(peek(), message);
        advance();
    }

    @Contract("_, _ -> new")
    private @NotNull ParseError error(@NotNull Token token, String message) {
        String errorMessage;
        if (token.type() == EOF)
            errorMessage = message + " at end";
        else
            errorMessage = message + " at '" + token.lexeme() + "'";
        diagnosticCollector.error(errorMessage, token.pos());
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON || previous().type() == COLON)
                return;
            advance();
        }
    }

    private boolean match(TokenType @NotNull ... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private static class ParseError extends RuntimeException {
    }
}
