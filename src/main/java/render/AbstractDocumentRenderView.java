package render;

import model.Document;
import model.Formatting;
import model.FormattingSpan;
import model.Paragraph;
import model.Section;
import model.SectionElement;
import service.StyleRegistry;

import java.util.ArrayList;
import java.util.List;

/** Shared recursive traversal for document presentation views. */
abstract class AbstractDocumentRenderView implements DocumentRenderView {
    private final StyleRegistry styles;

    AbstractDocumentRenderView(StyleRegistry styles) { this.styles = styles; }

    @Override public String render(Document document, int width, LineBreakStrategy strategy) {
        StringBuilder output = new StringBuilder();
        for (Section section : document.getSections()) renderSection(section, 0, width, strategy, output);
        if (output.length() > 0 && output.charAt(output.length() - 1) == '\n') output.setLength(output.length() - 1);
        return output.toString();
    }

    private void renderSection(Section section, int depth, int width, LineBreakStrategy strategy, StringBuilder output) {
        int indent = depth * 2;
        appendBlock(section.getTitle(), styles.resolveSection(section), List.of(), indent, width, strategy, output);
        for (SectionElement element : section.getElements()) {
            if (element instanceof Paragraph paragraph) {
                Formatting base = styles.resolveParagraph(paragraph);
                appendBlock(paragraph.getText(), base, paragraph.getFormattedContent().getFormattingSpans(), indent + 2,
                        width, strategy, output);
            } else if (element instanceof Section nested) {
                renderSection(nested, depth + 1, width, strategy, output);
            }
        }
    }

    private void appendBlock(String text, Formatting base, List<FormattingSpan> spans, int indent,
                             int width, LineBreakStrategy strategy, StringBuilder output) {
        int actualIndent = Math.min(indent, Math.max(0, width - 1));
        int available = width - actualIndent;
        List<BrokenLine> lines = strategy.breakWithOffsets(text, available);
        if (lines.isEmpty()) lines = List.of(new BrokenLine(0, 0, ""));
        for (BrokenLine line : lines) {
            output.append(" ".repeat(actualIndent));
            output.append(formatLine(text, line, base, spans));
            output.append('\n');
        }
    }

    private String formatLine(String text, BrokenLine line, Formatting base, List<FormattingSpan> spans) {
        if (!usesMarkup()) return line.getDisplayText();
        if (line.getStartOffset() == line.getEndOffset()) return line.getDisplayText();
        StringBuilder result = new StringBuilder();
        int cursor = line.getStartOffset();
        int end = line.getEndOffset();
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(cursor);
        boundaries.add(end);
        for (FormattingSpan span : spans) {
            if (span.getEnd() > cursor && span.getStart() < end) {
                boundaries.add(Math.max(cursor, span.getStart()));
                boundaries.add(Math.min(end, span.getEnd()));
            }
        }
        boundaries = boundaries.stream().distinct().sorted().toList();
        for (int i = 0; i < boundaries.size() - 1; i++) {
            int left = boundaries.get(i);
            int right = boundaries.get(i + 1);
            Formatting effective = base.copy();
            for (FormattingSpan span : spans) {
                if (left >= span.getStart() && left < span.getEnd()) effective.apply(span.getFormatting());
            }
            effective = effective.resolveAgainst(null);
            String segment = text.substring(left, right);
            result.append(openMarkers(effective)).append(segment).append(closeMarkers(effective));
        }
        if (line.getDisplayText().endsWith("-") && !text.substring(line.getStartOffset(), line.getEndOffset()).endsWith("-")) {
            result.append('-');
        }
        return result.toString();
    }

    protected abstract boolean usesMarkup();
    protected abstract String openMarkers(Formatting formatting);
    protected abstract String closeMarkers(Formatting formatting);
}