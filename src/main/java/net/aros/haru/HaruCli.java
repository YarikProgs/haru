package net.aros.haru;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Instruction;
import net.aros.haru.compile.io.ProgramIO;
import net.aros.haru.diagnostic.Diagnostic;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.diagnostic.DiagnosticRenderer;
import net.aros.haru.lex.IdentifierCollector;
import net.aros.haru.link.LinkedProgram;
import net.aros.haru.runtime.AbstractDialoguePlayer;
import net.aros.haru.runtime.DialogueException;
import net.aros.haru.translation.TranslationFile;
import net.aros.haru.translation.io.TranslationFileIO;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;

@CommandLine.Command(
        name = "haru",
        mixinStandardHelpOptions = true,
        version = "haru 1.0.0",
        description = "A dialogue-specific programming language",
        sortOptions = false,
        optionListHeading = "%nOptions:%n",
        parameterListHeading = "%nArguments:%n"
)
public class HaruCli implements Callable<Integer> {
    public static final int EX_NOINPUT = 60;
    public static final int EX_DATAERR = 65;

    @CommandLine.Parameters(arity = "1", paramLabel = "<path>", description = "Path to .haru file")
    private Path path;

    @CommandLine.Option(names = {"-t", "--translation"}, description = "Path to the translation file. Default: <path>.tr.json")
    private Path translationPath;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Path to the binary file. Default: <path>.haruc")
    private Path outputPath;

    @CommandLine.Option(names = {"-i", "--show-instructions"}, description = "Show instructions on compilation. Default: no")
    private boolean showInstructions;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Always recompile even if fresh binary is present. Default: no")
    private boolean force;

    @CommandLine.Option(arity = "0..", names = {"-l", "--locale"}, description = "Add locale into translation file")
    private List<String> locales;

    @CommandLine.Option(arity = "0..", names = {"-e", "--external-var"}, description = "Declare external variable")
    private List<String> externalVariables;

    public static void main(String[] args) {
        System.exit(new CommandLine(new HaruCli()).execute(args));
    }

    private void showInstructions(@NotNull CompiledProgram program) {
        System.out.println("\nInstructions:");
        for (Instruction instruction : program.instructions()) {
            System.out.println("* " + instruction);
        }
    }

    private void export(String source, CompiledProgram program) {
        try (OutputStream stream = Files.newOutputStream(translationPath)) {
            TranslationFileIO.exportFile(program.translationFile(), stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (OutputStream stream = Files.newOutputStream(outputPath)) {
            ProgramIO.exportFile(source, program, stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer call() throws IOException {
        int ex = checkPaths();
        if (ex != 0) return ex;

        if (externalVariables == null)
            externalVariables = List.of();
        if (locales == null)
            locales = List.of();

        return compileFile();
    }

    private int checkPaths() {
        if (Files.notExists(path)) {
            System.err.println("File not found: " + path);
            return EX_NOINPUT;
        }
        if (!Files.isRegularFile(path)) {
            System.err.println("Not a file: " + path);
            return EX_NOINPUT;
        }
        int ex = checkPath(translationPath, () -> translationPath = makeTranslationPath(path));
        if (ex != 0) return ex;
        return checkPath(outputPath, () -> outputPath = makeOutputPath(path));
    }

    private Path makeTranslationPath(Path path) {
        String filename = path.getFileName().toString();
        String sibling = filename.endsWith(".haru")
                ? filename.substring(0, filename.length() - 5) + ".tr.json"
                : filename + ".tr.json";
        return path.resolveSibling(sibling);
    }

    private Path makeOutputPath(Path path) {
        String filename = path.getFileName().toString();
        String sibling = filename.endsWith(".haru")
                ? filename + "c"
                : filename + ".haruc";
        return path.resolveSibling(sibling);
    }

    private int checkPath(Path dir, Runnable onNull) {
        if (dir == null) {
            onNull.run();
            return 0;
        }
        if (Files.exists(dir) && !Files.isRegularFile(dir)) {
            System.err.println("Not a file: " + dir);
            return EX_NOINPUT;
        }
        return 0;
    }

    private int compileFile() throws IOException {
        DiagnosticCollector diagnosticCollector = new DiagnosticCollector();
        String source = Files.readString(path);
        int status = compileOrImport(source, diagnosticCollector);
//        printDiagnostics(diagnosticCollector.diagnostics());
        System.out.println(DiagnosticRenderer.render(diagnosticCollector.diagnostics(), path, source));
        return status;
    }

    private Optional<CompiledProgram> importProgram(String source, TranslationFile translationFile, DiagnosticCollector diagnosticCollector) {
        if (force) return Optional.empty();
        if (!Files.exists(outputPath))
            return Optional.empty();
        try (InputStream bis = Files.newInputStream(outputPath)) {
            if (!ProgramIO.isFreshHeader(source, bis, diagnosticCollector))
                return Optional.empty();
            return Optional.of(ProgramIO.importFile(translationFile, bis));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private int compileOrImport(String source, DiagnosticCollector diagnosticCollector) {
        Optional<TranslationFile> translationFile = importTranslationFile();
        Optional<CompiledProgram> compiledProgram = translationFile
                .flatMap(tf -> importProgram(source, tf, diagnosticCollector))
                .or(() -> compile(source, diagnosticCollector, translationFile.orElse(null)));

        if (compiledProgram.isEmpty()) {
            return EX_DATAERR;
        }

        LinkedProgram program = Haru.link(compiledProgram.get(), diagnosticCollector, UnaryOperator.identity()).orElse(null);
        if (program == null) return EX_DATAERR;

        if (showInstructions) showInstructions(program.program());
        export(source, program.program());

        runProgram(program);
        return 0;
    }

    private Optional<CompiledProgram> compile(String source, DiagnosticCollector diagnosticCollector, TranslationFile translationFile) {
        var ic = new IdentifierCollector();
        Optional<CompiledProgram> compile = Haru.compile(
                source,
                diagnosticCollector,
                ic,
                compiler -> compiler.withLocales(locales).declareExternalAll(externalVariables),
                translationFile
        );
        System.out.println(ic.occurrencesOf("aros"));
        return compile;
    }

    private Optional<TranslationFile> importTranslationFile() {
        if (translationPath == null || Files.notExists(translationPath))
            return Optional.empty();
        try (var stream = Files.newInputStream(translationPath)) {
            return TranslationFileIO.importFile(stream).result();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExampleDialoguePlayer extends AbstractDialoguePlayer {
        public ExampleDialoguePlayer(@NotNull LinkedProgram program) throws DialogueException {
            super(program, Map.of());
        }

        @Override
        public void onSingleLine(Value[] args) {
            System.out.println("Line: " + render(args));
            resume();
        }

        @Override
        public void onMultipleLines(Value[][] args) {
            System.out.println("Parallel(");
            for (Value[] arg : args) {
                System.out.println("* Line: " + render(arg));
            }
            System.out.println(")");
            resume();
        }

        private String render(Value arg) {
            if (arg.isBool()) return Boolean.toString(arg.asBool());
            if (arg.isNull()) return "null";
            if (arg.isNumber()) return String.valueOf(arg.asNumber());
            if (arg.isLiteralRef())
                return program().program().literals()[arg.asLiteralRef()];
            if (arg.isTextRef())
                return program().program().translationFile().entries().get(arg.asTextRef()).translations().get("ru_ru");
            return arg.toString();
        }

        private String render(Value[] args) {
            StringBuilder builder = new StringBuilder();
            for (Value arg : args) {
                builder.append(render(arg)).append("; ");
            }
            return builder.toString();
        }
    }

    private void runProgram(LinkedProgram program) {
        System.out.println("\nRunning:");
        try {
            var player = new ExampleDialoguePlayer(program);
            while (!player.isDone()) {
                player.update();
            }
        } catch (DialogueException e) {
            System.err.println(e.diagnostic());
            throw new RuntimeException(e);
        }
    }

//    private void printDiagnostics(@NotNull List<Diagnostic> diagnostics) {
//        for (Diagnostic diagnostic : diagnostics) {
//            StringBuilder msg = new StringBuilder();
//            msg.append("[").append(diagnostic.severity()).append("] ").append(path.toString());
//            if (diagnostic.span() != null) {
//                msg.append(":").append(diagnostic.span().line()).append(":").append(diagnostic.span().column());
//            }
//            msg.append(": ").append(diagnostic.message());
//            System.err.println(msg);
//        }
//    }
}
