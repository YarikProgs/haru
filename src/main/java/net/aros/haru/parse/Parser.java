package net.aros.haru.parse;

import net.aros.haru.Haru;
import net.aros.haru.ast.Expr;
import net.aros.haru.ast.Program;
import net.aros.haru.ast.Stmt;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Token;
import net.aros.haru.lex.TokenType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
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
                statements.add(namedBlockOrStatement());
            } catch (ParseError ignored) {
                synchronize();
            }
        }
        return new Program(statements);
    }

    private Stmt namedBlockOrStatement() {
        if (match(IDENTIFIER)) {
            Token ident = previous();
            if (check(LBRACE)) return new Stmt.NamedBlock(ident, block());
            if (check(COMMA)) return line(new Expr.Variable(ident));
            return exprStmt(new Expr.Variable(ident));
        }
        return statement();
    }

    private Stmt statement() {
        if (match(LET)) return declaration();
        if (match(DO)) return doBlock();
        if (match(IF)) return ifStmt();
        if (match(PARALLEL)) return system();
        if (match(END)) return new Stmt.End(previous());

        Expr expr = expression();
        if (check(COMMA)) return line(expr);
        return exprStmt(expr);
    }

    @Contract(" -> new")
    private @NotNull Stmt doBlock() {
        Token ident = consume(IDENTIFIER, "Expect name of block after 'do'");
        consume(SEMICOLON, "Expect ';' after block name");
        return new Stmt.Do(ident);
    }

    @Contract(" -> new")
    private @NotNull Stmt declaration() {
        Token let = previous();
        Token name = consume(IDENTIFIER, "Expect variable name after 'let'");
        consume(EQUAL, "Expect '=' after variable name");
        var expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Declaration(let, name, expr);
    }

    @Contract(" -> new")
    private @NotNull Stmt ifStmt() {
        Token tok = previous();
        Expr cond = expression();
        Stmt.Block then = block();
        Stmt.Block orElse = match(ELIF) ? new Stmt.Block(null, List.of(ifStmt()), null) : match(ELSE) ? block() : null;
        return new Stmt.If(tok, cond, then, orElse);
    }

    private Stmt.@NotNull Block block() {
        Token rbrace = consume(LBRACE, "Expect '{'");
        List<Stmt> stmts = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            stmts.add(statement());
        }
        Token lbrace = consume(RBRACE, "Expect '}'");
        return new Stmt.Block(lbrace, Collections.unmodifiableList(stmts), rbrace);
    }

    @Contract(" -> new")
    private @NotNull Stmt system() {
        Token parallel = previous();
        consume(LBRACE, "Expect '{' after 'parallel'");
        List<Stmt.Line> lines = new ArrayList<>();
        do {
            lines.add(line(expression()));
        } while (!check(RBRACE) && !isAtEnd());
        Token rbrace = consume(RBRACE, "Expect '}' after lines");
        return new Stmt.System(parallel, lines, rbrace);
    }

    @Contract("_ -> new")
    private Stmt.@NotNull Line line(Expr first) {
        List<Expr> items = new ArrayList<>();
        items.add(first);
        while (match(COMMA)) {
            if (check(SEMICOLON)) break;
            items.add(expression());
        }
        consume(SEMICOLON, "Expect ';' after last element of a line");
        return new Stmt.Line(items);
    }

    @Contract("_ -> new")
    private @NotNull Stmt exprStmt(Expr expr) {
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.ExprStmt(expr);
    }

    @Contract(" -> new")
    private @NotNull Expr expression() {
        return primary();
    }

    private @NotNull Expr primary() {
        if (match(NULL)) return new Expr.Literal(previous(), null);
        if (match(TRUE)) return new Expr.Literal(previous(), true);
        if (match(FALSE)) return new Expr.Literal(previous(), false);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous(), previous().literal());
        if (match(TEXT)) return new Expr.Literal(previous(), Haru.parseText(previous(), diagnosticCollector));
        if (match(IDENTIFIER)) {
            Token ident = previous();
            if (match(LPAREN)) return finishCall(ident);
            return new Expr.Variable(ident);
        }
        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "Expect ')' after expression");
            return expr;
        }
        throw error(peek(), "Expect expression");
    }

    @Contract("_ -> new")
    private @NotNull Expr finishCall(Token callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                if (arguments.size() >= 255) {
                    //noinspection ThrowableNotThrown
                    error(peek(), "Can't have more than 255 arguments");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token rparen = consume(RPAREN, "Expect ')' after arguments");
        return new Expr.Call(callee, arguments, rparen);
    }

    private Token consume(TokenType type, String message) {
        if (!check(type)) throw error(peek(), message);
        return advance();
    }

    @Contract("_, _ -> new")
    private @NotNull ParseError error(@NotNull Token token, String message) {
        String errorMessage;
        if (token.type() == EOF)
            errorMessage = message + " at end";
        else
            errorMessage = message + " at '" + token.lexeme() + "'";
        diagnosticCollector.error(errorMessage, token.span());
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
