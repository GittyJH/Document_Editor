package model;

/** Identifies a half-open matching range within a paragraph. */
public final class Match {
    private final Paragraph paragraph;
    private final int startOffset;
    private final int endOffset;

    /** Creates a match for a valid range in the supplied paragraph. */
    public Match(Paragraph paragraph, int startOffset, int endOffset) {
        if (paragraph == null) throw new IllegalArgumentException("Paragraph cannot be null.");
        if (startOffset < 0 || endOffset <= startOffset || endOffset > paragraph.getText().length()) {
            throw new IllegalArgumentException("Invalid match range.");
        }
        this.paragraph = paragraph;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    /** Returns the paragraph containing the match. */ public Paragraph getParagraph() { return paragraph; }
    /** Returns the inclusive match start offset. */ public int getStartOffset() { return startOffset; }
    /** Returns the exclusive match end offset. */ public int getEndOffset() { return endOffset; }
}