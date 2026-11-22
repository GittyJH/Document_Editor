package render;

import java.util.ArrayList;
import java.util.List;

/** Strategy abstraction for converting text blocks into lines of a requested width. */
public abstract class LineBreakStrategy {
    /** Breaks text into display lines. */
    public abstract List<String> breakLine(String text, int width);

    /** Breaks text while retaining source offsets for formatted rendering. */
    public List<BrokenLine> breakWithOffsets(String text, int width) {
        List<String> lines = breakLine(text, width);
        List<BrokenLine> result = new ArrayList<>();
        int cursor = 0;
        for (String line : lines) {
            int start = text.indexOf(line, cursor);
            if (start < 0) start = cursor;
            int end = Math.min(text.length(), start + line.length());
            result.add(new BrokenLine(start, end, line));
            cursor = end;
        }
        return result;
    }

    /** Validates the requested line width. */
    public static boolean validateWidth(int width) { return width > 0; }
}