package model;

/** Common abstraction for composite document nodes that expose text. */
public abstract class SectionElement {
    private Formatting formatting = new Formatting();

    /** Returns the textual content represented by this element. */
    public abstract String getText();

    /** Returns the text with its explicit formatting shown using markers. */
    public abstract String getFormattedText();

    /** Returns the explicitly assigned element-level formatting. */
    public Formatting getFormatting() { return formatting.copy(); }

    /** Replaces the element-level formatting with a defensive copy. */
    public void setFormatting(Formatting formatting) {
        if (formatting == null) throw new IllegalArgumentException("Formatting cannot be null.");
        this.formatting = formatting.copy();
    }

    /** Removes one explicitly assigned element-level property. */
    public void clearFormatting(FormatProperty property) { formatting.clear(property); }

    /** Returns the mutable internal formatting for subclasses. */
    protected Formatting formattingCopy() { return formatting.copy(); }

    /** Copies the base formatting into another element. */
    protected void copyBaseTo(SectionElement target) { target.setFormatting(formatting); }
}