package model;

/** A reusable named collection of formatting properties. */
public final class Style {
    private final String name;
    private Formatting properties;

    /** Creates a named style with the supplied formatting properties. */
    public Style(String name, Formatting properties) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Style name cannot be empty.");
        if (properties == null) throw new IllegalArgumentException("Style properties cannot be null.");
        this.name = name.trim();
        this.properties = properties.copy();
    }

    /** Returns this style's name. */ public String getName() { return name; }
    /** Returns a defensive copy of the style's formatting properties. */ public Formatting getProperties() { return properties.copy(); }
    /** Replaces this style's properties; assigned sections observe the updated values. */
    public void setProperties(Formatting properties) {
        if (properties == null) throw new IllegalArgumentException("Style properties cannot be null.");
        this.properties = properties.copy();
    }
    /** Adds or replaces explicitly specified style properties. */ public void apply(Formatting patch) { properties.apply(patch); }
}