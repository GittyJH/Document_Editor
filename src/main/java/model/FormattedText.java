package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Stores paragraph characters separately from non-overlapping formatting spans. */
public final class FormattedText {
    private String text;
    private List<FormattingSpan> spans;

    /** Creates empty formatted text. */
    public FormattedText() { this(""); }

    /** Creates formatted text with no explicit formatting. */
    public FormattedText(String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.spans = new ArrayList<>();
    }

    private FormattedText(String text, List<FormattingSpan> spans) {
        this.text = text;
        this.spans = new ArrayList<>(spans);
    }

    /** Returns the plain characters. */
    public String getText() { return text; }
    /** Returns the number of characters. */
    public int length() { return text.length(); }
    /** Returns an immutable copy of the explicit formatting spans. */
    public List<FormattingSpan> getFormattingSpans() { return List.copyOf(spans); }

    /** Returns an independent copy preserving text and formatting spans. */
    public FormattedText copy() { return new FormattedText(text, copySpans(spans)); }

    /** Inserts text at a valid offset, inheriting the adjacent character formatting when possible. */
    public void insert(int offset, String insertedText) {
        Objects.requireNonNull(insertedText, "insertedText");
        validateRange(offset, offset);
        if (insertedText.isEmpty()) return;
        Formatting inherited = formattingAtInsertion(offset);
        replaceRange(offset, offset, insertedText);
        if (!inherited.isEmpty()) applyFormatting(offset, offset + insertedText.length(), inherited);
    }

    /** Deletes a valid half-open range of characters. */
    public void delete(int start, int end) { replaceRange(start, end, ""); }

    /** Replaces a valid half-open range while preserving unaffected formatting metadata. */
    public void replaceRange(int start, int end, String replacementText) {
        Objects.requireNonNull(replacementText, "replacementText");
        validateRange(start, end);
        if (start == end && replacementText.isEmpty()) return;
        Formatting inherited = formattingAtInsertion(start);
        int delta = replacementText.length() - (end - start);
        List<FormattingSpan> changed = new ArrayList<>();
        for (FormattingSpan span : spans) {
            int spanStart = span.getStart();
            int spanEnd = span.getEnd();
            Formatting value = span.getFormatting();
            if (spanEnd <= start) {
                changed.add(new FormattingSpan(spanStart, spanEnd, value));
            } else if (spanStart >= end) {
                changed.add(new FormattingSpan(spanStart + delta, spanEnd + delta, value));
            } else {
                if (spanStart < start) changed.add(new FormattingSpan(spanStart, start, value));
                if (spanEnd > end) {
                    int rightStart = start + replacementText.length();
                    changed.add(new FormattingSpan(rightStart, spanEnd + delta, value));
                }
            }
        }
        text = text.substring(0, start) + replacementText + text.substring(end);
        spans = normalize(changed);
        if (!replacementText.isEmpty() && !inherited.isEmpty()) {
            applyFormatting(start, start + replacementText.length(), inherited);
        }
    }

    /** Moves a range; the destination is interpreted in the original pre-move text. */
    public void moveRange(int start, int end, int destination) {
        validateRange(start, end);
        if (destination < 0 || destination > text.length()) throw new IllegalArgumentException("Destination is outside the paragraph.");
        if (destination > start && destination < end) throw new IllegalArgumentException("Destination cannot be inside the moved range.");
        if (start == end || destination == start || destination == end) return;
        FormattedText fragment = slice(start, end);
        String moved = text.substring(start, end);
        replaceRange(start, end, "");
        int adjusted = destination > end ? destination - (end - start) : destination;
        replaceRange(adjusted, adjusted, moved);
        clearAllFormatting(adjusted, adjusted + moved.length());
        for (FormattingSpan span : fragment.spans) {
            applyFormatting(adjusted + span.getStart(), adjusted + span.getEnd(), span.getFormatting());
        }
    }

    /** Applies the supplied properties across a valid nonempty range. */
    public void applyFormatting(int start, int end, Formatting formatting) {
        Objects.requireNonNull(formatting, "formatting");
        validateRange(start, end);
        if (start == end || formatting.isEmpty()) return;
        rebuildRange(start, end, formatting, null);
    }

    /** Removes every explicit formatting override from a valid range. */
    public void clearAllFormatting(int start, int end) {
        validateRange(start, end);
        if (start == end) return;
        List<FormattingSpan> result = new ArrayList<>();
        for (FormattingSpan span : spans) {
            if (span.getEnd() <= start || span.getStart() >= end) {
                result.add(new FormattingSpan(span.getStart(), span.getEnd(), span.getFormatting()));
            } else {
                if (span.getStart() < start) result.add(new FormattingSpan(span.getStart(), start, span.getFormatting()));
                if (span.getEnd() > end) result.add(new FormattingSpan(end, span.getEnd(), span.getFormatting()));
            }
        }
        spans = normalize(result);
    }

    /** Removes one explicit property from a range so it can inherit its value. */
    public void clearFormatting(int start, int end, FormatProperty property) {
        Objects.requireNonNull(property, "property");
        validateRange(start, end);
        if (start == end) return;
        rebuildRange(start, end, null, property);
    }

    /** Returns the explicit override active at a character position. */
    public Formatting getFormattingAt(int offset) {
        if (offset < 0 || offset >= text.length()) return new Formatting();
        for (FormattingSpan span : spans) {
            if (offset >= span.getStart() && offset < span.getEnd()) return span.getFormatting();
        }
        return new Formatting();
    }

    /** Returns a copied slice with formatting offsets rebased to zero. */
    public FormattedText slice(int start, int end) {
        validateRange(start, end);
        List<FormattingSpan> result = new ArrayList<>();
        for (FormattingSpan span : spans) {
            int left = Math.max(start, span.getStart());
            int right = Math.min(end, span.getEnd());
            if (left < right) result.add(new FormattingSpan(left - start, right - start, span.getFormatting()));
        }
        return new FormattedText(text.substring(start, end), result);
    }

    /** Appends another formatted value, shifting its spans to the new end. */
    public void append(FormattedText other) {
        Objects.requireNonNull(other, "other");
        int offset = text.length();
        text += other.text;
        for (FormattingSpan span : other.spans) {
            spans.add(new FormattingSpan(offset + span.getStart(), offset + span.getEnd(), span.getFormatting()));
        }
        spans = normalize(spans);
    }

    private void rebuildRange(int start, int end, Formatting patch, FormatProperty clear) {
        TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(0);
        boundaries.add(text.length());
        boundaries.add(start);
        boundaries.add(end);
        for (FormattingSpan span : spans) {
            boundaries.add(span.getStart());
            boundaries.add(span.getEnd());
        }
        List<Integer> points = new ArrayList<>(boundaries);
        List<FormattingSpan> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            int left = points.get(i);
            int right = points.get(i + 1);
            if (left == right) continue;
            Formatting value = getFormattingAt(left);
            if (left >= start && right <= end) {
                if (patch != null) value.apply(patch);
                if (clear != null) value.clear(clear);
            }
            if (!value.isEmpty()) result.add(new FormattingSpan(left, right, value));
        }
        spans = normalize(result);
    }

    private Formatting formattingAtInsertion(int offset) {
        if (text.isEmpty()) return new Formatting();
        if (offset < text.length()) return getFormattingAt(offset);
        return getFormattingAt(offset - 1);
    }

    private void validateRange(int start, int end) {
        if (start < 0 || end < start || end > text.length()) {
            throw new IllegalArgumentException("Invalid text range [" + start + ", " + end + ") for length " + text.length() + ".");
        }
    }

    private static List<FormattingSpan> normalize(List<FormattingSpan> source) {
        if (source.isEmpty()) return new ArrayList<>();
        source.sort(Comparator.comparingInt(FormattingSpan::getStart));
        List<FormattingSpan> result = new ArrayList<>();
        for (FormattingSpan current : source) {
            if (current.getStart() == current.getEnd() || current.getFormatting().isEmpty()) continue;
            if (!result.isEmpty()) {
                FormattingSpan previous = result.get(result.size() - 1);
                if (previous.getEnd() == current.getStart()
                        && previous.getFormatting().equals(current.getFormatting())) {
                    result.set(result.size() - 1, new FormattingSpan(previous.getStart(), current.getEnd(), previous.getFormatting()));
                    continue;
                }
                if (previous.getEnd() > current.getStart()) throw new IllegalStateException("Overlapping formatting spans.");
            }
            result.add(new FormattingSpan(current.getStart(), current.getEnd(), current.getFormatting()));
        }
        return result;
    }

    private static List<FormattingSpan> copySpans(List<FormattingSpan> source) {
        List<FormattingSpan> result = new ArrayList<>();
        for (FormattingSpan span : source) result.add(new FormattingSpan(span.getStart(), span.getEnd(), span.getFormatting()));
        return result;
    }
}