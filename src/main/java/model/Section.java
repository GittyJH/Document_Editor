package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A composite node containing paragraphs and arbitrarily nested sections. */
public final class Section extends SectionElement {
    private String title;
    private final List<SectionElement> children = new ArrayList<>();
    private Section parent;
    private String styleName;

    /** Creates a section with a nonblank title. */
    public Section(String title) { setTitle(title); }

    /** Returns the section title. */ public String getTitle() { return title; }
    /** Replaces the section title with a nonblank value. */
    public void setTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Section title cannot be empty.");
        this.title = title.trim();
    }
    /** Returns the parent section, or {@code null} for a root section. */ public Section getParent() { return parent; }
    /** Returns the name of the style assigned to this section, if any. */ public String getStyleName() { return styleName; }
    /** Assigns a named style; {@code null} removes the section's style. */ public void setStyleName(String styleName) { this.styleName = styleName == null || styleName.isBlank() ? null : styleName.trim(); }

    /** Adds a nested section and returns it. */
    public Section addSection(String title) {
        Section section = new Section(title);
        section.parent = this;
        children.add(section);
        return section;
    }

    /** Adds a paragraph and returns it. */
    public Paragraph addParagraph(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.attachTo(this);
        children.add(paragraph);
        return paragraph;
    }

    /** Adds an existing child element while preserving single-parent ownership. */
    public void addElement(SectionElement element) {
        Objects.requireNonNull(element, "element");
        if (children.contains(element)) throw new IllegalArgumentException("Element is already in this section.");
        if (element instanceof Paragraph paragraph && paragraph.getParentSection() != null) {
            throw new IllegalArgumentException("Paragraph already belongs to a section.");
        }
        if (element instanceof Section section) {
            if (section.parent != null) throw new IllegalArgumentException("Section already belongs to a section.");
            if (isAncestorOrSelf(section)) throw new IllegalArgumentException("Adding this section would create a document cycle.");
        }
        if (element instanceof Paragraph paragraph) paragraph.attachTo(this);
        if (element instanceof Section section) section.parent = this;
        children.add(element);
    }


    /** Inserts a detached element immediately after an existing direct child. */
    public void insertAfter(SectionElement anchor, SectionElement element) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(element, "element");
        int index = children.indexOf(anchor);
        if (index < 0) throw new IllegalArgumentException("Anchor is not a child of this section.");
        if (element instanceof Paragraph paragraph && paragraph.getParentSection() != null) {
            throw new IllegalArgumentException("Paragraph already belongs to a section.");
        }
        if (element instanceof Section section) {
            if (section.parent != null) throw new IllegalArgumentException("Section already belongs to a section.");
            if (isAncestorOrSelf(section)) throw new IllegalArgumentException("Adding this section would create a document cycle.");
        }
        if (element instanceof Paragraph paragraph) paragraph.attachTo(this);
        if (element instanceof Section section) section.parent = this;
        children.add(index + 1, element);
    }

    /** Returns the direct child immediately following {@code element}, or {@code null}. */
    public SectionElement getNextSibling(SectionElement element) {
        int index = children.indexOf(element);
        if (index < 0) throw new IllegalArgumentException("Element is not a child of this section.");
        return index + 1 < children.size() ? children.get(index + 1) : null;
    }

    /** Removes a direct child element; a non-child is rejected. */
    public void removeElement(SectionElement element) {
        if (!children.remove(element)) throw new IllegalArgumentException("Element is not a child of this section.");
        if (element instanceof Paragraph paragraph) paragraph.attachTo(null);
        if (element instanceof Section section) section.parent = null;
    }

    /** Returns an immutable view of the direct children in document order. */
    public List<SectionElement> getElements() { return Collections.unmodifiableList(children); }

    /** Returns the title, which is this composite node's own textual value. */
    @Override public String getText() { return title; }
    /** Returns the section title with its explicit formatting represented by visible markers. */
    @Override public String getFormattedText() {
        Formatting formatting = getFormatting();
        return FormattingMarkup.open(formatting) + title + FormattingMarkup.close(formatting);
    }

    /** Returns all paragraphs within this section in depth-first document order. */
    public List<Paragraph> getParagraphs() {
        List<Paragraph> result = new ArrayList<>();
        collectParagraphs(this, result);
        return Collections.unmodifiableList(result);
    }

    /** Returns a deep copy of this section and every descendant. */
    public Section deepCopy() {
        Section copy = new Section(title);
        copyBaseTo(copy);
        copy.styleName = styleName;
        for (SectionElement child : children) {
            if (child instanceof Section section) copy.addElement(section.deepCopy());
            else if (child instanceof Paragraph paragraph) copy.addElement(paragraph.deepCopy());
        }
        return copy;
    }

    private boolean isAncestorOrSelf(Section candidate) {
        Section current = this;
        while (current != null) {
            if (current == candidate) return true;
            current = current.parent;
        }
        return false;
    }

    private static void collectParagraphs(Section section, List<Paragraph> result) {
        for (SectionElement child : section.children) {
            if (child instanceof Paragraph paragraph) result.add(paragraph);
            else if (child instanceof Section nested) collectParagraphs(nested, result);
        }
    }
}