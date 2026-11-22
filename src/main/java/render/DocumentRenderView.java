package render;

import model.Document;

/** Pluggable presentation view that renders a document without mutating it. */
public interface DocumentRenderView {
    /** Renders the document using the supplied width and line-breaking strategy. */
    String render(Document document, int width, LineBreakStrategy strategy);
}