package model;

/** Internal helper that translates resolved formatting metadata into visible text markers. */
final class FormattingMarkup {
    private FormattingMarkup() { }

    static String open(Formatting source) {
        Formatting formatting = source.resolveAgainst(null);
        StringBuilder marker = new StringBuilder();
        if (Boolean.TRUE.equals(formatting.getBold())) marker.append("**");
        if (Boolean.TRUE.equals(formatting.getItalic())) marker.append('*');
        if (!"Arial".equals(formatting.getFontName()) || !"black".equals(formatting.getColor())
                || !Integer.valueOf(12).equals(formatting.getFontSize())) {
            marker.append("{font=").append(formatting.getFontName()).append(",size=")
                    .append(formatting.getFontSize()).append(",color=").append(formatting.getColor()).append('}');
        }
        return marker.toString();
    }

    static String close(Formatting source) {
        Formatting formatting = source.resolveAgainst(null);
        StringBuilder marker = new StringBuilder();
        if (!"Arial".equals(formatting.getFontName()) || !"black".equals(formatting.getColor())
                || !Integer.valueOf(12).equals(formatting.getFontSize())) marker.append("{/font}");
        if (Boolean.TRUE.equals(formatting.getItalic())) marker.append('*');
        if (Boolean.TRUE.equals(formatting.getBold())) marker.append("**");
        return marker.toString();
    }
}