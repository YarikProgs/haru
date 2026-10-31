package net.aros.haru.ast;

import net.aros.haru.runtime.DialogueException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: javadocs
public sealed interface TextPart {
    // abc
    record Literal(String part) implements TextPart {
    }

    // {var}
    record Placeholder(String name) implements TextPart {
    }

    // <a arg1 arg2>inner</a>
    record BiTag(String tag, List<String> arguments, TextPart inner) implements TextPart {
    }

    // <a/ arg1 arg2>
    record Tag(String tag, List<String> arguments) implements TextPart {
    }

    // <a>{b}</a>hey!
    record Parts(List<TextPart> parts) implements TextPart {
    }

    interface Visitor {
        void visitLiteral(Literal textPart) throws DialogueException;
        void visitPlaceholder(Placeholder textPart) throws DialogueException;
        void visitBiTag(BiTag textPart) throws DialogueException;
        void visitTag(Tag textPart) throws DialogueException;
        void visitParts(Parts textPart) throws DialogueException;

        default void visit(@NotNull TextPart textPart) throws DialogueException {
            switch (textPart) {
                case Literal literal -> visitLiteral(literal);
                case Placeholder placeholder -> visitPlaceholder(placeholder);
                case BiTag biTag -> visitBiTag(biTag);
                case Tag tag -> visitTag(tag);
                case Parts parts -> visitParts(parts);
            }
        }
    }
}
