package net.aros.haru;

import net.aros.haru.ast.Program;
import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Compiler;
import net.aros.haru.compile.Instruction;
import net.aros.haru.diagnostic.Diagnostic;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.lex.Lexer;
import net.aros.haru.lex.Token;
import net.aros.haru.parse.Parser;
import net.aros.haru.runtime.AbstractDialoguePlayer;
import net.aros.haru.runtime.DialogueException;
import net.aros.haru.value.HaruFunction;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HaruCli {
    public static final int EX_USAGE = 2;
    public static final int EX_NOINPUT = 60;

    public static void main(String[] args) throws IOException {
        switch (args.length) {
            case 0 -> repl();
            case 1 -> runFile(args[0]);
            default -> {
                System.out.println("Usage: [haru] (path)");
                System.exit(EX_USAGE);
            }
        }
    }

    private static void repl() throws IOException {
        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = scanner.readLine();
            if (line == null) break;
            if (line.isBlank()) continue;
            run("<script>", line);
        }
    }

    private static void runFile(String stringPath) throws IOException {
        Path path = Paths.get(stringPath);
        if (Files.notExists(path)) {
            System.out.println("File not found");
            System.exit(EX_NOINPUT);
        }
        run(stringPath, Files.readString(path));
    }

    private static void run(String filename, String source) {
        DiagnosticCollector diagnosticCollector = new DiagnosticCollector();

        List<Token> tokens = new Lexer(diagnosticCollector, source).lexTokens();
        if (diagnosticCollector.hasErrors()) {
            printDiagnostics(filename, diagnosticCollector.diagnostics());
            return;
        }
        Program program = new Parser(diagnosticCollector, tokens).parse();
        if (diagnosticCollector.hasErrors()) {
            printDiagnostics(filename, diagnosticCollector.diagnostics());
            return;
        }
        CompiledProgram compiledProgram = new Compiler(diagnosticCollector, program)
                .declareFunction("add1", new HaruFunction() {
                    @Override
                    public Value call(Value arg1) {
                        if (arg1.isNumber()) return Value.number(arg1.asNumber() + 1);
                        return Value.nullValue();
                    }
                })
                .compile();
        System.out.println("Diagnostics:");
        printDiagnostics(filename, diagnosticCollector.diagnostics());
        if (diagnosticCollector.hasErrors()) {
            return;
        }
        System.out.println("\nInstructions:");
        for (Instruction instruction : compiledProgram.instructions()) {
            System.out.println("* " + instruction);
        }
        System.out.println("\nRunning:");
        try {
            var player = new ExampleDialoguePlayer(compiledProgram);
            while (!player.isDone()) {
                player.update();
            }
        } catch (DialogueException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExampleDialoguePlayer extends AbstractDialoguePlayer {
        public ExampleDialoguePlayer(@NotNull CompiledProgram program) throws DialogueException {
            super(program, Map.of());
        }

        @Override
        public void onLine(Value[] args) {
            System.out.println("Line: " + Arrays.toString(args));
            resume();
        }

        @Override
        public void onLines(Value[][] args) {
            System.out.println("Parallel(");
            for (Value[] arg : args) {
                System.out.println("* Line: " + Arrays.toString(arg));
            }
            System.out.println(")");
            resume();
        }
    }

    private static void printDiagnostics(String filename, @NotNull List<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            System.out.printf("[%s] %s:%d:%d: %s\n", diagnostic.severity(), filename, diagnostic.pos().line(), diagnostic.pos().column(), diagnostic.message());
        }
    }
}
