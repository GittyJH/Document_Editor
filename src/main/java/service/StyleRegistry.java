package service;

import model.Formatting;
import model.Paragraph;
import model.Section;
import model.SectionElement;
import model.Style;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Stores named styles and resolves inherited properties through nested sections. */
public final class StyleRegistry {
    /** Immutable snapshot of all registered style properties. */
    public static final class State {
        private final Map<String, Formatting> values;
        private State(Map<String, Formatting> values) {
            this.values = new LinkedHashMap<>();
            values.forEach((name, formatting) -> this.values.put(name, formatting.copy()));
        }
        /** Returns a defensive map of style definitions captured by this state. */
        public Map<String, Formatting> getStyles() {
            Map<String, Formatting> copy = new LinkedHashMap<>();
            values.forEach((name, formatting) -> copy.put(name, formatting.copy()));
            return Collections.unmodifiableMap(copy);
        }
    }

    private final Map<String, Style> styles = new LinkedHashMap<>();

    /** Defines or replaces a named style. Existing section references observe changes immediately. */
    public Style define(String name, Formatting properties) {
        Style style = new Style(name, properties);
        styles.put(style.getName(), style);
        return style;
    }

    /** Returns a style by name, rejecting unknown names. */
    public Style get(String name) {
        Style style = styles.get(name);
        if (style == null) throw new IllegalArgumentException("Unknown style: " + name);
        return style;
    }

    /** Applies a named style to a section. */
    public void applyTo(Section section, String styleName) {
        Objects.requireNonNull(section, "section");
        get(styleName);
        section.setStyleName(styleName);
    }

    /** Returns an immutable list of registered style names. */ public java.util.List<String> getStyleNames() { return java.util.List.copyOf(styles.keySet()); }

    /** Returns an immutable snapshot of the current registry. */
    public State snapshot() {
        Map<String, Formatting> values = new LinkedHashMap<>();
        styles.forEach((name, style) -> values.put(name, style.getProperties()));
        return new State(values);
    }

    /** Restores all registered names and properties from a prior snapshot. */
    public void restore(State state) {
        styles.clear();
        state.values.forEach((name, formatting) -> styles.put(name, new Style(name, formatting)));
    }


    /** Resolves the inherited and element-level properties for a section title. */
    public Formatting resolveSection(Section section) {
        Formatting result = inheritedFor(section);
        result.apply(section.getFormatting());
        return result.resolveAgainst(null);
    }

    /** Resolves inherited section styles and paragraph-level overrides. */
    public Formatting resolveParagraph(Paragraph paragraph) {
        Formatting result = inheritedFor(paragraph.getParentSection());
        result.apply(paragraph.getElementFormatting());
        return result.resolveAgainst(null);
    }

    /** Resolves a paragraph's effective style with one additional character-range override. */
    public Formatting resolveParagraph(Paragraph paragraph, Formatting rangeOverride) {
        Formatting result = resolveParagraph(paragraph);
        result.apply(rangeOverride);
        return result.resolveAgainst(null);
    }

    private Formatting inheritedFor(Section section) {
        java.util.ArrayDeque<Section> ancestors = new java.util.ArrayDeque<>();
        Section current = section;
        while (current != null) {
            ancestors.push(current);
            current = current.getParent();
        }
        Formatting result = new Formatting();
        while (!ancestors.isEmpty()) {
            Section ancestor = ancestors.pop();
            String styleName = ancestor.getStyleName();
            if (styleName != null) result.apply(get(styleName).getProperties());
        }
        return result;
    }
}