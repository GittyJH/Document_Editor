package render;

import model.Formatting;
import service.StyleRegistry;

/** Marked-up presentation that makes bold, italic, font, size, and color visible. */
public final class MarkupRenderer extends AbstractDocumentRenderView {
    /** Creates a marked-up renderer using the supplied inherited-style registry. */
    public MarkupRenderer(StyleRegistry styles) { super(styles); }
    @Override protected boolean usesMarkup() { return true; }
    @Override protected String openMarkers(Formatting formatting) {
        StringBuilder marker = new StringBuilder();
        if (Boolean.TRUE.equals(formatting.getBold())) marker.append("**");
        if (Boolean.TRUE.equals(formatting.getItalic())) marker.append('*');
        if (!"Arial".equals(formatting.getFontName()) || !"black".equals(formatting.getColor()) || formatting.getFontSize() != 12) {
            marker.append("{font=").append(formatting.getFontName()).append(",size=")
                    .append(formatting.getFontSize()).append(",color=").append(formatting.getColor()).append('}');
        }
        return marker.toString();
    }
    @Override protected String closeMarkers(Formatting formatting) {
        StringBuilder marker = new StringBuilder();
        if (!"Arial".equals(formatting.getFontName()) || !"black".equals(formatting.getColor()) || formatting.getFontSize() != 12) marker.append("{/font}");
        if (Boolean.TRUE.equals(formatting.getItalic())) marker.append('*');
        if (Boolean.TRUE.equals(formatting.getBold())) marker.append("**");
        return marker.toString();
    }
}