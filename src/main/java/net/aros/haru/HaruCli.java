package net.aros.haru;

import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.compile.Instruction;
import net.aros.haru.diagnostic.Diagnostic;
import net.aros.haru.diagnostic.DiagnosticCollector;
import net.aros.haru.runtime.AbstractDialoguePlayer;
import net.aros.haru.runtime.DialogueException;
import net.aros.haru.translation.TranslationFile;
import net.aros.haru.translation.TranslationFileIO;
import net.aros.haru.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

    @CommandLine.Option(names = {"-t", "--translation"}, description = "Path to the translation file. Default: <path>.json")
    private Path translationPath;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Path to the binary file. Default: <path>.bin")
    private Path outputPath;

    @CommandLine.Option(names = {"-i", "--show-instructions"}, description = "Show instructions on compilation. Default: no")
    private boolean showInstructions;

    @CommandLine.Option(arity = "0..", names = {"-l", "--locale"}, description = "Add locale into translation file")
    private List<String> locales;

    @CommandLine.Option(arity = "0..", names = {"-e", "--external-var"}, description = "Add external variable")
    private List<String> externalVariables;

    public static void main(String[] args) {
        System.exit(new CommandLine(new HaruCli()).execute(args));
    }

    private void showInstructions(CompiledProgram program) {
        System.out.println("\nInstructions:");
        for (Instruction instruction : program.instructions()) {
            System.out.println("* " + instruction);
        }
    }

    private void export(CompiledProgram program) {
        try (OutputStream stream = Files.newOutputStream(translationPath)) {
            TranslationFileIO.exportFile(program.translationFile(), stream);
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
        int ex = checkPath(translationPath, () -> translationPath = Paths.get(path.toString() + ".json"));
        if (ex != 0) return ex;
        return checkPath(outputPath, () -> outputPath = Paths.get(path.toString() + ".bin"));
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
        return compile(Files.readString(path));
    }

    private int compile(String source) {
        var diagnosticCollector = new DiagnosticCollector();
        var result = Haru.compile(
                source,
                diagnosticCollector,
                compiler -> compiler.withLocales(locales).declareExternalVariables(externalVariables),
                importTranslationFile()
        );
        printDiagnostics(diagnosticCollector.diagnostics());
        if (result.isEmpty()) return EX_DATAERR;

        if (showInstructions) showInstructions(result.get());
        export(result.get());

        runProgram(result.get());
        return 0;
    }

    private @Nullable TranslationFile importTranslationFile() {
        if (translationPath == null || Files.notExists(translationPath)) return null;
        try (var stream = Files.newInputStream(translationPath)) {
            return TranslationFileIO.importFile(stream).getOrThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExampleDialoguePlayer extends AbstractDialoguePlayer {
        public ExampleDialoguePlayer(@NotNull CompiledProgram program) throws DialogueException {
            super(program, Map.of());
        }

        @Override
        public void onLine(Value[] args) {
            System.out.println("Line: " + render(args));
            resume();
        }

        @Override
        public void onLines(Value[][] args) {
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
            if (arg.isString()) return arg.asString();
            if (arg.isTextRef())
                return program().translationFile().entries().get(arg.asTextRef()).translations().get("ru_ru");
            return arg.toString();
        }

        private String render(Value[] args) {
            StringBuilder builder = new StringBuilder();
            for (Value arg : args) {
                builder.append(render(arg)).append("[,]");
            }
            return builder.toString();
        }
    }

    private void runProgram(CompiledProgram program) {
        System.out.println("\nRunning:");
        try {
            var player = new ExampleDialoguePlayer(program);
            while (!player.isDone()) {
                player.update();
            }
        } catch (DialogueException e) {
            throw new RuntimeException(e);
        }
    }

    private void printDiagnostics(@NotNull List<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            System.out.printf("[%s] %s:%d:%d: %s\n", diagnostic.severity(), path.toString(), diagnostic.pos().line(), diagnostic.pos().column(), diagnostic.message());
        }
    }
}
