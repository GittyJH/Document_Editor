package model;

/** An immutable half-open text range carrying a partial formatting override. */
public final class FormattingSpan {
    private final int start;
    private final int end;
    private final Formatting formatting;

    /** Creates a formatting span. */
    public FormattingSpan(int start, int end, Formatting formatting) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid formatting span.");
        this.start = start;
        this.end = end;
        this.formatting = formatting.copy();
    }

    /** Returns the inclusive start offset. */
    public int getStart() { return start; }
    /** Returns the exclusive end offset. */
    public int getEnd() { return end; }
    /** Returns a defensive copy of the formatting override. */
    public Formatting getFormatting() { return formatting.copy(); }
}