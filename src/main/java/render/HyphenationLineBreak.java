package render;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Line-breaking strategy that splits words wider than the available line width. */
public final class HyphenationLineBreak extends LineBreakStrategy {
    private static final Pattern WORD = Pattern.compile("\\S+");

    /** Breaks long words into width-limited pieces joined by visible hyphens. */
    @Override public List<String> breakLine(String text, int width) {
        List<String> result = new ArrayList<>();
        for (BrokenLine line : breakWithOffsets(text, width)) result.add(line.getDisplayText());
        return List.copyOf(result);
    }

    /** Returns offset-aware hyphenated lines for formatted rendering. */
    @Override public List<BrokenLine> breakWithOffsets(String text, int width) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null.");
        if (width <= 0) throw new IllegalArgumentException("Line width must be positive.");
        List<BrokenLine> result = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        StringBuilder line = new StringBuilder();
        int lineStart = -1;
        int lineEnd = -1;
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > width) {
                if (line.length() > 0) {
                    result.add(new BrokenLine(lineStart, lineEnd, line.toString()));
                    line.setLength(0);
                    lineStart = -1;
                }
                int offset = matcher.start();
                int remaining = word.length();
                while (remaining > width) {
                    int pieceLength = width == 1 ? 1 : width - 1;
                    String piece = word.substring(offset - matcher.start(), offset - matcher.start() + pieceLength)
                            + (width == 1 ? "" : "-");
                    result.add(new BrokenLine(offset, offset + pieceLength, piece));
                    offset += pieceLength;
                    remaining -= pieceLength;
                }
                String tail = word.substring(word.length() - remaining);
                result.add(new BrokenLine(offset, matcher.end(), tail));
                continue;
            }
            int proposed = line.length() == 0 ? word.length() : line.length() + 1 + word.length();
            if (line.length() > 0 && proposed > width) {
                result.add(new BrokenLine(lineStart, lineEnd, line.toString()));
                line.setLength(0);
                lineStart = -1;
            }
            if (line.length() == 0) {
                lineStart = matcher.start();
                lineEnd = matcher.end();
                line.append(word);
            } else {
                line.append(' ').append(word);
                lineEnd = matcher.end();
            }
        }
        if (line.length() > 0) result.add(new BrokenLine(lineStart, lineEnd, line.toString()));
        return List.copyOf(result);
    }
}