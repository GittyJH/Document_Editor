package command;

import model.Document;
import model.Section;
import service.StyleRegistry;

/** Reversible command for document title, section, and paragraph structure changes. */
public final class StructureCommand extends AbstractDocumentCommand {
    /** Structure operation supported by this command. */
    public enum Kind { CREATE_DOCUMENT, SET_TITLE, ADD_SECTION, ADD_PARAGRAPH, REMOVE_SECTION }

    private final Kind kind;
    private final String path;
    private final String value;

    /** Creates a structure command. Use {@code root} as the parent path for a top-level section. */
    public StructureCommand(Document document, StyleRegistry styles, Kind kind, String path, String value) {
        super(document, styles);
        if (kind == null) throw new IllegalArgumentException("Structure operation is required.");
        this.kind = kind;
        this.path = path;
        this.value = value == null ? "" : value;
    }

    @Override protected void perform() {
        switch (kind) {
            case CREATE_DOCUMENT -> {
                document.setTitle(value);
                for (Section section : document.getSections().toArray(Section[]::new)) document.removeSection(section);
            }
            case SET_TITLE -> document.setTitle(value);
            case ADD_SECTION -> {
                if (path == null || path.equalsIgnoreCase("root")) document.addSection(value);
                else document.getSectionByPath(path).addSection(value);
            }
            case ADD_PARAGRAPH -> document.getSectionByPath(path).addParagraph(value);
            case REMOVE_SECTION -> {
                Section target = document.getSectionByPath(path);
                Section parent = target.getParent();
                if (parent == null) document.removeSection(target);
                else parent.removeElement(target);
            }
        }
    }

    /** Returns the structure operation represented by this command. */ public Kind getKind() { return kind; }

    @Override public String serialize() {
        return switch (kind) {
            case CREATE_DOCUMENT -> "document create " + EditCommand.quote(value);
            case SET_TITLE -> "document title " + EditCommand.quote(value);
            case ADD_SECTION -> "section add " + (path == null ? "root" : path) + " " + EditCommand.quote(value);
            case ADD_PARAGRAPH -> "paragraph add " + path + " " + EditCommand.quote(value);
            case REMOVE_SECTION -> "section remove " + path;
        };
    }

    @Override public String description() { return kind + (path == null ? "" : " " + path); }
}