package render;

import service.StyleRegistry;

/** Plain-text presentation that deliberately omits all formatting markers. */
public final class PlainTextRenderer extends AbstractDocumentRenderView {
    /** Creates a plain-text renderer using the supplied inherited-style registry. */
    public PlainTextRenderer(StyleRegistry styles) { super(styles); }
    @Override protected boolean usesMarkup() { return false; }
    @Override protected String openMarkers(model.Formatting formatting) { return ""; }
    @Override protected String closeMarkers(model.Formatting formatting) { return ""; }
}