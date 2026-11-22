package render;

import model.Document;
import service.StyleRegistry;

import java.util.EnumMap;
import java.util.Map;

/** Coordinates selectable document views while delegating line breaking to a strategy. */
public final class Renderer {
    private final Map<RenderMode, DocumentRenderView> views = new EnumMap<>(RenderMode.class);

    /** Creates a renderer with the built-in plain-text and marked-up views. */
    public Renderer(StyleRegistry styles) {
        views.put(RenderMode.PLAIN_TEXT, new PlainTextRenderer(styles));
        views.put(RenderMode.MARKED_UP, new MarkupRenderer(styles));
    }

    /** Registers or replaces a view for a render mode. */
    public void registerView(RenderMode mode, DocumentRenderView view) {
        if (mode == null || view == null) throw new IllegalArgumentException("Render mode and view are required.");
        views.put(mode, view);
    }

    /** Renders a document in the selected mode and line-breaking strategy. */
    public String render(Document document, RenderMode mode, int width, LineBreakStrategy strategy) {
        if (document == null || mode == null || strategy == null) throw new IllegalArgumentException("Document, mode, and strategy are required.");
        if (!validateWidth(width)) throw new IllegalArgumentException("Line width must be positive.");
        DocumentRenderView view = views.get(mode);
        if (view == null) throw new IllegalArgumentException("No renderer is registered for " + mode + ".");
        return view.render(document, width, strategy);
    }

    /** Returns whether a line width is valid. */ public boolean validateWidth(int width) { return width > 0; }
}