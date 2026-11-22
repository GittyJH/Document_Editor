package service;

import model.Document;
import model.Match;
import model.MatchMode;

import java.util.List;

/** Provides search operations independently of editing commands and rendering. */
public final class SearchService {
    /** Finds all matches in document order across every nested section. */
    public List<Match> find(Document document, String query, MatchMode mode) {
        if (document == null) throw new IllegalArgumentException("Document cannot be null.");
        return document.findMatches(query, mode);
    }

    /** Returns the first match, or {@code null} when no match exists. */
    public Match findFirst(Document document, String query, MatchMode mode) {
        List<Match> matches = find(document, query, mode);
        return matches.isEmpty() ? null : matches.get(0);
    }
}