package command;

import model.Document;
import model.FormatProperty;
import model.Formatting;
import model.Section;
import service.StyleRegistry;

import java.util.Locale;

/** Reversible command for defining, updating, and applying named inherited styles. */
public final class StyleCommand extends AbstractDocumentCommand {
    /** The supported style operation. */ public enum Kind { DEFINE, APPLY }
    private final Kind kind;
    private final String name;
    private final String path;
    private final FormatProperty property;
    private final String value;

    /** Creates a style definition or update command. */
    public StyleCommand(Document document, StyleRegistry styles, String name, FormatProperty property, String value) {
        super(document, styles);
        this.kind = Kind.DEFINE;
        this.name = name;
        this.path = null;
        this.property = property;
        this.value = value;
        validate();
    }

    /** Creates a command applying an existing named style to a section. */
    public StyleCommand(Document document, StyleRegistry styles, String path, String name) {
        super(document, styles);
        this.kind = Kind.APPLY;
        this.name = name;
        this.path = path;
        this.property = null;
        this.value = null;
        validate();
    }

    @Override protected void perform() {
        if (kind == Kind.APPLY) {
            styles.applyTo(document.getSectionByPath(path), name);
        } else {
            Formatting current;
            try { current = styles.get(name).getProperties(); }
            catch (IllegalArgumentException missing) { current = new Formatting(); }
            current.set(property, value);
            styles.define(name, current);
        }
    }

    private void validate() {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Style name cannot be empty.");
        if (kind == Kind.APPLY && (path == null || path.isBlank())) throw new IllegalArgumentException("Section path is required.");
        if (kind == Kind.DEFINE && (property == null || value == null || value.isBlank())) throw new IllegalArgumentException("Style property and value are required.");
    }

    /** Returns the style operation type. */ public Kind getKind() { return kind; }
    @Override public String serialize() {
        if (kind == Kind.APPLY) return "style apply " + path + " " + EditCommand.quote(name);
        return "style define " + EditCommand.quote(name) + " " + property.name().toLowerCase(Locale.ROOT) + " " + EditCommand.quote(value);
    }
    @Override public String description() { return kind + " style " + name; }
}