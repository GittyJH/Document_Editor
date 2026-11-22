package model;

import java.util.Locale;

/** Identifies one property supported by character formatting. */
public enum FormatProperty {
    /** Bold text. */ BOLD,
    /** Italic text. */ ITALIC,
    /** Font family name. */ FONT,
    /** Font size in points. */ SIZE,
    /** Text color. */ COLOR;

    /** Parses a property name, accepting common command-line aliases. */
    public static FormatProperty fromString(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "bold" -> BOLD;
            case "italic" -> ITALIC;
            case "font", "fontname", "font-name" -> FONT;
            case "size", "fontsize", "font-size" -> SIZE;
            case "color", "colour" -> COLOR;
            default -> throw new IllegalArgumentException("Unknown formatting property: " + value);
        };
    }
}