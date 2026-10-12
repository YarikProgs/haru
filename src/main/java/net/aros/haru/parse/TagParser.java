package net.aros.haru.parse;

import net.aros.haru.ast.Tag;

public class TagParser {
    public static final int RADIX = 2 * 26 + 10;

    public static Tag parse(String stringTag) throws InvalidTagException {
        long value = 0;
        for (char ch : stringTag.toCharArray()) {
            value = value * RADIX + base36to10(ch);
        }
        return new Tag(value);
    }

    public static String toString(Tag tag, int len) {
        StringBuilder builder = new StringBuilder();
        long value = tag.uid();
        if (value < 0)
            throw new IllegalStateException("Tag cannot be negative");
        while (value > 0) {
            int rem = (int) (value % RADIX);
            builder.append(base10to36(rem));
            value /= RADIX;
        }
        String result = builder.reverse().toString();
        if (builder.length() > len)
            throw new IllegalStateException("Tag is larger than maximum length. Expected " + len + ", got: " + builder.length() + " (" + result + ")");
        if (builder.length() == len)
            return result;
        return "0".repeat(len - builder.length()) + result;
    }

    private static int base36to10(char ch) throws InvalidTagException {
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'a' && ch <= 'z') return ch - 'a' + 10;
        if (ch >= 'A' && ch <= 'Z') return ch - 'A' + 10 + 26;
        throw new InvalidTagException("Not a valid tag character: '" + ch + "'");
    }

    private static char base10to36(int n) {
        if (n >= 0 && n <= 9) return (char) (n + '0');
        if (n >= 10 && n < 36) return (char) (n + 'a' - 10);
        if (n >= 10+26 && n < RADIX) return (char) (n + 'A' - 10 - 26);
        throw new IllegalStateException("Not a valid tag digit: " + n);
    }
}
