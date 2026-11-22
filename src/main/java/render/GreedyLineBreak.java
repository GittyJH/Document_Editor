package render;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Greedy line breaking that places as many complete words as possible on each line. */
public final class GreedyLineBreak extends LineBreakStrategy {
    private static final Pattern WORD = Pattern.compile("\\S+");

    /** Breaks text greedily without splitting words. A word wider than the line is kept intact. */
    @Override public List<String> breakLine(String text, int width) {
        validate(text, width);
        List<String> lines = new ArrayList<>();
        for (BrokenLine line : breakWithOffsetsInternal(text, width)) lines.add(line.getDisplayText());
        return List.copyOf(lines);
    }

    /** Returns offset-aware greedy line segments. */
    @Override public List<BrokenLine> breakWithOffsets(String text, int width) {
        validate(text, width);
        return List.copyOf(breakWithOffsetsInternal(text, width));
    }

    private List<BrokenLine> breakWithOffsetsInternal(String text, int width) {
        List<BrokenLine> lines = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        int lineStart = -1;
        int lineEnd = -1;
        StringBuilder line = new StringBuilder();
        while (matcher.find()) {
            String word = matcher.group();
            int proposed = line.length() == 0 ? word.length() : line.length() + 1 + word.length();
            if (line.length() > 0 && proposed > width) {
                lines.add(new BrokenLine(lineStart, lineEnd, line.toString()));
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
        if (line.length() > 0) lines.add(new BrokenLine(lineStart, lineEnd, line.toString()));
        return lines;
    }

    private static void validate(String text, int width) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null.");
        if (width <= 0) throw new IllegalArgumentException("Line width must be positive.");
    }
}