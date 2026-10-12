package net.aros.haru;

import net.aros.haru.ast.Tag;
import net.aros.haru.parse.InvalidTagException;
import net.aros.haru.parse.TagParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TagTest {
    @Test
    public void testParse() {
        Assertions.assertEquals(TagParser.RADIX * TagParser.RADIX - 1, TagParser.parse("ZZ").uid());
        Assertions.assertEquals(0, TagParser.parse("0").uid());

        Assertions.assertThrows(InvalidTagException.class, () -> TagParser.parse("?"));
        Assertions.assertDoesNotThrow(() -> TagParser.parse("l"));
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("0ZZ", TagParser.toString(new Tag(TagParser.RADIX * TagParser.RADIX - 1), 3));
        Assertions.assertEquals("0000", TagParser.toString(new Tag(0), 4));

        Assertions.assertThrows(IllegalStateException.class, () -> TagParser.toString(new Tag(-1), 1));
        Assertions.assertThrows(IllegalStateException.class, () -> TagParser.toString(new Tag(999999), 2));
    }
}
