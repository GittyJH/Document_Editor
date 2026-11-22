package command;

import model.Paragraph;
import model.Section;
import model.SectionElement;
import model.Document;
import service.StyleRegistry;

/** Reversible command for insertion, deletion, replacement, movement, split, and merge. */
public final class EditCommand extends AbstractDocumentCommand {
    private final EditOperation operation;
    private final int paragraphIndex;
    private final int start;
    private final int end;
    private final int destination;
    private final String text;

    /** Creates an edit command targeting a paragraph by its depth-first index. */
    public EditCommand(Document document, StyleRegistry styles, EditOperation operation,
                       int paragraphIndex, int start, int end, int destination, String text) {
        super(document, styles);
        if (operation == null) throw new IllegalArgumentException("Edit operation is required.");
        this.operation = operation;
        this.paragraphIndex = paragraphIndex;
        this.start = start;
        this.end = end;
        this.destination = destination;
        this.text = text == null ? "" : text;
    }

    /** Creates an insert command. */
    public static EditCommand insert(Document document, StyleRegistry styles, int paragraph, int offset, String text) {
        return new EditCommand(document, styles, EditOperation.INSERT, paragraph, offset, offset, -1, text);
    }

    /** Creates a delete command. */
    public static EditCommand delete(Document document, StyleRegistry styles, int paragraph, int start, int end) {
        return new EditCommand(document, styles, EditOperation.DELETE, paragraph, start, end, -1, "");
    }

    /** Creates a replace command. */
    public static EditCommand replace(Document document, StyleRegistry styles, int paragraph, int start, int end, String text) {
        return new EditCommand(document, styles, EditOperation.REPLACE, paragraph, start, end, -1, text);
    }

    /** Creates a move command. */
    public static EditCommand move(Document document, StyleRegistry styles, int paragraph, int start, int end, int destination) {
        return new EditCommand(document, styles, EditOperation.MOVE, paragraph, start, end, destination, "");
    }

    /** Creates a split command. */
    public static EditCommand split(Document document, StyleRegistry styles, int paragraph, int offset) {
        return new EditCommand(document, styles, EditOperation.SPLIT, paragraph, offset, offset, -1, "");
    }

    /** Creates a merge command. */
    public static EditCommand merge(Document document, StyleRegistry styles, int paragraph) {
        return new EditCommand(document, styles, EditOperation.MERGE, paragraph, 0, 0, -1, "");
    }

    /** Validates a half-open text range before it is modified. */
    public boolean validatePosition(int start, int end, int length) {
        return start >= 0 && end >= start && end <= length;
    }

    @Override protected void perform() {
        Paragraph paragraph = document.getParagraph(paragraphIndex);
        if (operation == EditOperation.INSERT) {
            if (!validatePosition(start, start, paragraph.length())) throw new IllegalArgumentException("Insert position is outside the paragraph.");
            paragraph.insert(start, text);
        } else if (operation == EditOperation.DELETE || operation == EditOperation.REPLACE) {
            if (!validatePosition(start, end, paragraph.length())) throw new IllegalArgumentException("Text range is outside the paragraph.");
            if (operation == EditOperation.DELETE) paragraph.delete(start, end);
            else paragraph.replaceRange(start, end, text);
        } else if (operation == EditOperation.MOVE) {
            if (!validatePosition(start, end, paragraph.length()) || destination < 0 || destination > paragraph.length()) {
                throw new IllegalArgumentException("Move range or destination is outside the paragraph.");
            }
            paragraph.moveRange(start, end, destination);
        } else if (operation == EditOperation.SPLIT) {
            if (!validatePosition(start, start, paragraph.length())) throw new IllegalArgumentException("Split position is outside the paragraph.");
            Section parent = paragraph.getParentSection();
            if (parent == null) throw new IllegalStateException("Paragraph is not attached to a section.");
            Paragraph suffix = paragraph.splitAt(start);
            parent.insertAfter(paragraph, suffix);
        } else if (operation == EditOperation.MERGE) {
            Section parent = paragraph.getParentSection();
            if (parent == null) throw new IllegalStateException("Paragraph is not attached to a section.");
            SectionElement next = parent.getNextSibling(paragraph);
            if (!(next instanceof Paragraph following)) throw new IllegalArgumentException("No immediately following paragraph exists in this section.");
            paragraph.merge(following);
            parent.removeElement(following);
        }
    }

    /** Returns the operation kind. */ public EditOperation getOperation() { return operation; }
    /** Returns the target paragraph index. */ public int getParagraphIndex() { return paragraphIndex; }
    /** Returns a serialized command that can be parsed against an empty or equivalent document. */
    @Override public String serialize() {
        return switch (operation) {
            case INSERT -> "insert " + paragraphIndex + " " + start + " " + quote(text);
            case DELETE -> "delete " + paragraphIndex + " " + start + " " + end;
            case REPLACE -> "edit-replace " + paragraphIndex + " " + start + " " + end + " " + quote(text);
            case MOVE -> "move " + paragraphIndex + " " + start + " " + end + " " + destination;
            case SPLIT -> "split " + paragraphIndex + " " + start;
            case MERGE -> "merge " + paragraphIndex;
        };
    }
    /** Returns a short operation description. */
    @Override public String description() { return operation + " paragraph " + paragraphIndex; }

    /** Quotes and escapes one string argument for the text command syntax. */
    public static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}