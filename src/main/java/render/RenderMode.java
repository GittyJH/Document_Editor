package render;

/** Selects the rendering view used to display document content. */
public enum RenderMode {
    /** Text without formatting markers. */ PLAIN_TEXT,
    /** Text decorated with visible character-formatting markers. */ MARKED_UP
}