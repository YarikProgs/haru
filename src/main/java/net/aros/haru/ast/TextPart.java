package net.aros.haru.ast;

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

    interface Visitor<T> {
        T visitLiteral(Literal textPart);
        T visitPlaceholder(Placeholder textPart);
        T visitBiTag(BiTag textPart);
        T visitTag(Tag textPart);
        T visitParts(Parts textPart);

        default T visit(@NotNull TextPart textPart) {
            return switch (textPart) {
                case Literal literal -> visitLiteral(literal);
                case Placeholder placeholder -> visitPlaceholder(placeholder);
                case BiTag biTag -> visitBiTag(biTag);
                case Tag tag -> visitTag(tag);
                case Parts parts -> visitParts(parts);
            };
        }
    }
}
