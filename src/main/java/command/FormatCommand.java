package command;

import model.Document;
import model.FormatProperty;
import model.Formatting;
import model.Paragraph;
import model.Section;
import service.StyleRegistry;

import java.util.Locale;

/** Reversible command applying or removing character-formatting metadata. */
public final class FormatCommand extends AbstractDocumentCommand {
    private final Integer paragraphIndex;
    private final String sectionPath;
    private final int start;
    private final int end;
    private final FormatProperty property;
    private final String value;
    private final boolean clear;

    /** Creates a paragraph-range formatting command. */
    public FormatCommand(Document document, StyleRegistry styles, int paragraphIndex,
                         int start, int end, FormatProperty property, String value, boolean clear) {
        super(document, styles);
        this.paragraphIndex = paragraphIndex;
        this.sectionPath = null;
        this.start = start;
        this.end = end;
        this.property = property;
        this.value = value;
        this.clear = clear;
        validateArguments();
    }

    /** Creates a section-title formatting command that applies to the whole title. */
    public FormatCommand(Document document, StyleRegistry styles, String sectionPath,
                         FormatProperty property, String value, boolean clear) {
        super(document, styles);
        this.paragraphIndex = null;
        this.sectionPath = sectionPath;
        this.start = 0;
        this.end = 0;
        this.property = property;
        this.value = value;
        this.clear = clear;
        validateArguments();
    }

    @Override protected void perform() {
        if (sectionPath != null) {
            Section section = document.getSectionByPath(sectionPath);
            Formatting formatting = section.getFormatting();
            if (clear) formatting.clear(property); else formatting.set(property, value);
            section.setFormatting(formatting);
        } else {
            Paragraph paragraph = document.getParagraph(paragraphIndex);
            if (start < 0 || end < start || end > paragraph.length()) throw new IllegalArgumentException("Formatting range is outside the paragraph.");
            if (clear) paragraph.clearFormatting(start, end, property);
            else {
                Formatting formatting = new Formatting();
                formatting.set(property, value);
                paragraph.applyFormatting(start, end, formatting);
            }
        }
    }

    private void validateArguments() {
        if (property == null) throw new IllegalArgumentException("Formatting property is required.");
        if (!clear && (value == null || value.isBlank())) throw new IllegalArgumentException("Formatting value cannot be empty.");
        if (paragraphIndex == null && (sectionPath == null || sectionPath.isBlank())) throw new IllegalArgumentException("Section path is required.");
    }

    /** Returns whether this command targets a section title. */ public boolean targetsSectionTitle() { return sectionPath != null; }
    @Override public String serialize() {
        String action = clear ? "inherit" : EditCommand.quote(value);
        if (sectionPath != null) return "format-title " + sectionPath + " " + property.name().toLowerCase(Locale.ROOT) + " " + action;
        return "format " + paragraphIndex + " " + start + " " + end + " " + property.name().toLowerCase(Locale.ROOT) + " " + action;
    }
    @Override public String description() { return "format " + (sectionPath == null ? "paragraph " + paragraphIndex : "section " + sectionPath); }
}