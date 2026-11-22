package render;

/** A rendered line and its source offsets in the original text block. */
public final class BrokenLine {
    private final int startOffset;
    private final int endOffset;
    private final String displayText;

    /** Creates a broken-line value. */
    public BrokenLine(int startOffset, int endOffset, String displayText) {
        if (startOffset < 0 || endOffset < startOffset || displayText == null) {
            throw new IllegalArgumentException("Broken-line offsets and display text are invalid.");
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.displayText = displayText;
    }
    /** Returns the inclusive source start. */ public int getStartOffset() { return startOffset; }
    /** Returns the exclusive source end. */ public int getEndOffset() { return endOffset; }
    /** Returns the text displayed on this line, including any inserted hyphen. */ public String getDisplayText() { return displayText; }
}