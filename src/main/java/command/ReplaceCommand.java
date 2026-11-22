package command;

import model.Document;
import model.Match;
import model.MatchMode;
import model.Paragraph;
import service.StyleRegistry;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reversible find-and-replace command that treats all replacements as one operation. */
public final class ReplaceCommand extends AbstractDocumentCommand {
    private final List<Match> matches;
    private final String replacementText;
    private final String query;
    private final MatchMode mode;
    private final boolean replaceAll;

    /** Creates a replace command from a precomputed list of matches. */
    public ReplaceCommand(Document document, StyleRegistry styles, List<Match> matches,
                          String query, MatchMode mode, String replacementText, boolean replaceAll) {
        super(document, styles);
        if (matches == null || matches.isEmpty()) throw new IllegalArgumentException("No matching text was found.");
        if (replacementText == null) throw new IllegalArgumentException("Replacement text cannot be null.");
        this.matches = List.copyOf(matches);
        this.query = query;
        this.mode = mode;
        this.replacementText = replacementText;
        this.replaceAll = replaceAll;
    }

    @Override protected void perform() {
        List<Match> ordered = new ArrayList<>(matches);
        ordered.sort(Comparator.comparingInt((Match match) -> document.indexOf(match.getParagraph())).reversed()
                .thenComparing(Comparator.comparingInt(Match::getStartOffset).reversed()));
        for (Match match : ordered) {
            Paragraph paragraph = match.getParagraph();
            paragraph.replaceRange(match.getStartOffset(), match.getEndOffset(), replacementText);
        }
    }

    /** Returns the matches captured by this replace operation. */ public List<Match> getMatches() { return matches; }
    @Override public String serialize() {
        return (replaceAll ? "replace-all " : "replace-first ") + mode.name().toLowerCase(Locale.ROOT) + " "
                + EditCommand.quote(query) + " " + EditCommand.quote(replacementText);
    }
    @Override public String description() { return (replaceAll ? "Replace all " : "Replace first ") + query; }
}