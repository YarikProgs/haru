package net.aros.haru.runtime;

import net.aros.haru.ast.TextPart;
import net.aros.haru.compile.CompiledProgram;
import net.aros.haru.diagnostic.Severity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractTextRenderer implements TextPart.Visitor {
    private final AbstractDialoguePlayer player;
    private final StringBuilder builder = new StringBuilder();
    private final String locale;

    public AbstractTextRenderer(AbstractDialoguePlayer player, String locale) {
        this.player = player;
        this.locale = locale;
    }

    protected abstract void renderTagged(String tag, List<String> args, TextPart inner, StringBuilder into) throws DialogueException;

    protected abstract void renderTag(String tag, List<String> args, StringBuilder into) throws DialogueException;

    @Override
    public void visitLiteral(TextPart.@NotNull Literal textPart) {
        builder.append(textPart.part());
    }

    @Override
    public void visitPlaceholder(TextPart.@NotNull Placeholder textPart) throws DialogueException {
        int slot = getSlotByName(textPart.name());
        builder.append(player.renderValue(player.get(slot), locale));
    }

    @Override
    public void visitBiTag(TextPart.@NotNull BiTag textPart) throws DialogueException {
        renderTagged(textPart.tag(), textPart.arguments(), textPart.inner(), builder);
    }

    @Override
    public void visitTag(TextPart.@NotNull Tag textPart) throws DialogueException {
        renderTag(textPart.tag(), textPart.arguments(), builder);
    }

    @Override
    public void visitParts(TextPart.@NotNull Parts textPart) throws DialogueException {
        for (TextPart part : textPart.parts()) {
            visit(part);
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private CompiledProgram program() {
        return player.linkedProgram().program();
    }

    private int getSlotByName(String variableName) throws DialogueException {
        Integer slot = program().slots().get(variableName);
        if (slot != null)
            return slot;
        throw new DialogueException(new RuntimeDiagnostic(
                Severity.ERROR,
                RuntimeDiagnostic.ErrorCode.UNKNOWN_VARIABLE,
                "Unknown variable: '" + variableName + "'",
                null
        ));
    }

    public interface Factory {
        AbstractTextRenderer create(AbstractDialoguePlayer player, String locale);
    }
}
