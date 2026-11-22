package model;

import java.util.Objects;

/**
 * A partial character-formatting value. A {@code null} property means that the
 * value is unspecified and may therefore be inherited from a containing style.
 */
public final class Formatting {
    private Boolean bold;
    private Boolean italic;
    private String fontName;
    private Integer fontSize;
    private String color;

    /** Creates a formatting value with no explicitly specified properties. */
    public Formatting() {
    }

    /**
     * Creates a formatting value with the supplied optional properties.
     *
     * @param bold explicit bold value, or {@code null} to inherit
     * @param italic explicit italic value, or {@code null} to inherit
     * @param fontName explicit font name, or {@code null} to inherit
     * @param fontSize explicit positive font size, or {@code null} to inherit
     * @param color explicit color, or {@code null} to inherit
     */
    public Formatting(Boolean bold, Boolean italic, String fontName, Integer fontSize, String color) {
        if (fontSize != null && fontSize <= 0) {
            throw new IllegalArgumentException("Font size must be positive.");
        }
        this.bold = bold;
        this.italic = italic;
        this.fontName = blankToNull(fontName);
        this.fontSize = fontSize;
        this.color = blankToNull(color);
    }

    /** Returns the explicit bold property, or {@code null} when unspecified. */
    public Boolean getBold() { return bold; }

    /** Returns the explicit italic property, or {@code null} when unspecified. */
    public Boolean getItalic() { return italic; }

    /** Returns the explicit font name, or {@code null} when unspecified. */
    public String getFontName() { return fontName; }

    /** Returns the explicit font size, or {@code null} when unspecified. */
    public Integer getFontSize() { return fontSize; }

    /** Returns the explicit color, or {@code null} when unspecified. */
    public String getColor() { return color; }

    /**
     * Applies all explicitly specified properties from {@code other} to this
     * value, leaving unspecified properties unchanged.
     *
     * @param other formatting properties to overlay
     */
    public void apply(Formatting other) {
        Objects.requireNonNull(other, "other");
        if (other.bold != null) bold = other.bold;
        if (other.italic != null) italic = other.italic;
        if (other.fontName != null) fontName = other.fontName;
        if (other.fontSize != null) fontSize = other.fontSize;
        if (other.color != null) color = other.color;
    }

    /** Returns a copy with {@code other}'s explicitly specified values overlaid. */
    public Formatting mergedWith(Formatting other) {
        Formatting result = copy();
        result.apply(other);
        return result;
    }

    /** Returns an independent copy of this formatting value. */
    public Formatting copy() {
        return new Formatting(bold, italic, fontName, fontSize, color);
    }

    /** Returns whether this value has no explicitly specified properties. */
    public boolean isEmpty() {
        return bold == null && italic == null && fontName == null && fontSize == null && color == null;
    }

    /** Clears the named property so it can inherit from its parent. */
    public void clear(FormatProperty property) {
        Objects.requireNonNull(property, "property");
        switch (property) {
            case BOLD -> bold = null;
            case ITALIC -> italic = null;
            case FONT -> fontName = null;
            case SIZE -> fontSize = null;
            case COLOR -> color = null;
        }
    }

    /** Sets one explicitly specified property. */
    public void set(FormatProperty property, String value) {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(value, "value");
        switch (property) {
            case BOLD -> bold = parseBoolean(value);
            case ITALIC -> italic = parseBoolean(value);
            case FONT -> fontName = requireNonBlank(value, "Font name");
            case SIZE -> {
                try {
                    int parsed = Integer.parseInt(value);
                    if (parsed <= 0) throw new NumberFormatException();
                    fontSize = parsed;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Font size must be a positive integer.");
                }
            }
            case COLOR -> color = requireNonBlank(value, "Color");
        }
    }

    /** Returns a fully resolved format using the given inherited values and defaults. */
    public Formatting resolveAgainst(Formatting inherited) {
        Formatting result = new Formatting(false, false, "Arial", 12, "black");
        if (inherited != null) result.apply(inherited);
        result.apply(this);
        return result;
    }

    /** Returns whether the two formatting values contain the same properties. */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Formatting other)) return false;
        return Objects.equals(bold, other.bold) && Objects.equals(italic, other.italic)
                && Objects.equals(fontName, other.fontName) && Objects.equals(fontSize, other.fontSize)
                && Objects.equals(color, other.color);
    }

    /** Returns a hash code consistent with {@link #equals(Object)}. */
    @Override
    public int hashCode() { return Objects.hash(bold, italic, fontName, fontSize, color); }

    /** Returns a readable representation of the explicitly specified properties. */
    @Override
    public String toString() {
        return "Formatting{bold=" + bold + ", italic=" + italic + ", fontName='" + fontName
                + "', fontSize=" + fontSize + ", color='" + color + "'}";
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Expected true or false.");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireNonBlank(String value, String label) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(label + " cannot be empty.");
        return trimmed;
    }
}