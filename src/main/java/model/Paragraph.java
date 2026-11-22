package model;

import java.util.Objects;

/** A leaf node containing characters and compact character-formatting spans. */
public final class Paragraph extends SectionElement {
    private final FormattedText content;
    private Section parent;

    /** Creates an empty paragraph. */
    public Paragraph() { this(""); }

    /** Creates a paragraph containing the supplied text. */
    public Paragraph(String text) { this.content = new FormattedText(Objects.requireNonNull(text, "text")); }

    private Paragraph(FormattedText content) { this.content = content.copy(); }

    /** Returns the paragraph's plain characters. */
    @Override public String getText() { return content.getText(); }

    /** Returns a marker-based representation of explicit paragraph and character formatting. */
    @Override public String getFormattedText() {
        var spans = content.getFormattingSpans();
        java.util.TreeSet<Integer> boundaries = new java.util.TreeSet<>();
        boundaries.add(0);
        boundaries.add(length());
        for (FormattingSpan span : spans) {
            boundaries.add(span.getStart());
            boundaries.add(span.getEnd());
        }
        java.util.List<Integer> points = new java.util.ArrayList<>(boundaries);
        StringBuilder output = new StringBuilder();
        Formatting base = getElementFormatting();
        for (int i = 0; i < points.size() - 1; i++) {
            int left = points.get(i);
            int right = points.get(i + 1);
            if (left == right) continue;
            Formatting effective = base.copy();
            for (FormattingSpan span : spans) {
                if (left >= span.getStart() && left < span.getEnd()) effective.apply(span.getFormatting());
            }
            output.append(FormattingMarkup.open(effective))
                    .append(getText(), left, right)
                    .append(FormattingMarkup.close(effective));
        }
        return output.toString();
    }

    /** Returns the length of the paragraph. */ public int length() { return content.length(); }
    /** Returns an independent copy of the paragraph's formatted content. */ public FormattedText getFormattedContent() { return content.copy(); }
    /** Inserts characters at a valid offset. */ public void insert(int offset, String text) { content.insert(offset, text); }
    /** Deletes characters in a valid half-open range. */ public void delete(int start, int end) { content.delete(start, end); }
    /** Replaces characters in a valid half-open range. */ public void replaceRange(int start, int end, String replacementText) { content.replaceRange(start, end, replacementText); }
    /** Moves a range using a destination offset from the original paragraph state. */ public void moveRange(int start, int end, int destination) { content.moveRange(start, end, destination); }
    /** Applies formatting to a valid half-open range. */ public void applyFormatting(int start, int end, Formatting formatting) { content.applyFormatting(start, end, formatting); }
    /** Removes one explicit formatting property from a valid range. */ public void clearFormatting(int start, int end, FormatProperty property) { content.clearFormatting(start, end, property); }
    /** Removes all explicit range formatting from a valid range. */ public void clearAllFormatting(int start, int end) { content.clearAllFormatting(start, end); }

    /** Splits this paragraph at {@code offset}, returning the formatted suffix. */
    public Paragraph splitAt(int offset) {
        if (offset < 0 || offset > length()) throw new IllegalArgumentException("Split position is outside the paragraph.");
        FormattedText suffix = content.slice(offset, length());
        content.delete(offset, length());
        Paragraph result = new Paragraph(suffix);
        copyBaseTo(result);
        return result;
    }

    /** Merges the next paragraph's characters and formatting spans into this paragraph. */
    public void merge(Paragraph next) {
        Objects.requireNonNull(next, "next");
        content.append(next.content);
    }

    /** Returns the containing section, or {@code null} if not attached to a section. */
    public Section getParentSection() { return parent; }

    /** Sets the containing section; intended for document-tree assembly. */
    public void attachTo(Section section) { parent = section; }

    /** Returns the resolved formatting for the paragraph-level properties. */
    public Formatting getElementFormatting() { return formattingCopy(); }

    /** Returns a deep copy of this paragraph. */
    public Paragraph deepCopy() {
        Paragraph copy = new Paragraph(content);
        copyBaseTo(copy);
        return copy;
    }

}