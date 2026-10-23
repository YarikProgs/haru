package net.aros.haru;

import net.aros.haru.ast.Program;
import net.aros.haru.ast.TextPart;
import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Compiler;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Lexer;
import net.aros.haru.lex.Token;
import net.aros.haru.link.LinkedProgram;
import net.aros.haru.link.Linker;
import net.aros.haru.parse.ParsedText;
import net.aros.haru.parse.Parser;
import net.aros.haru.parse.TextParser;
import net.aros.haru.translation.TranslationFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Haru {
    public static @NotNull ParsedText parseText(@NotNull Token text, DiagnosticCollector diagnosticCollector) {
        if (!(text.literal() instanceof String raw))
            return new ParsedText("", new TextPart.Parts(List.of()));
        if (raw.indexOf('{') < 0 && raw.indexOf('<') < 0)
            return new ParsedText(raw, new TextPart.Literal(raw));

        return new TextParser(diagnosticCollector, text).parse();
    }

    public static Optional<CompiledProgram> compile(
            String source,
            DiagnosticCollector diagnosticCollector,
            @NotNull UnaryOperator<Compiler> compilerConfiguration,
            @Nullable TranslationFile oldTranslationFile
    ) {
        return lex(source, diagnosticCollector)
                .flatMap(tokens -> parse(tokens, diagnosticCollector))
                .flatMap(program -> compile(program, diagnosticCollector, compilerConfiguration, oldTranslationFile));
    }

    public static Optional<List<Token>> lex(String source, DiagnosticCollector diagnosticCollector) {
        List<Token> tokens = new Lexer(diagnosticCollector, source).lexTokens();
        if (diagnosticCollector.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(tokens);
    }

    public static Optional<Program> parse(List<Token> tokens, DiagnosticCollector diagnosticCollector) {
        Program program = new Parser(diagnosticCollector, tokens).parse();
        if (diagnosticCollector.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(program);
    }

    public static Optional<CompiledProgram> compile(
            Program program,
            DiagnosticCollector diagnosticCollector,
            @NotNull UnaryOperator<Compiler> compilerConfiguration,
            @Nullable TranslationFile oldTranslationFile
    ) {
        CompiledProgram cProgram = compilerConfiguration.apply(
                new Compiler(diagnosticCollector, program, oldTranslationFile)
        ).compile();
        if (diagnosticCollector.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(cProgram);
    }

    public static Optional<LinkedProgram> link(
            CompiledProgram program,
            DiagnosticCollector diagnosticCollector,
            @NotNull UnaryOperator<Linker> linkerConfiguration
    ) {
        LinkedProgram lProgram = linkerConfiguration.apply(
                new Linker(diagnosticCollector, program)
        ).link();
        if (diagnosticCollector.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(lProgram);
    }
}
