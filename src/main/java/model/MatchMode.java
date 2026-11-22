package model;

/** Selects how a text-search query is matched. */
public enum MatchMode {
    /** Matches every occurrence of the query inside a paragraph. */ SUBSTRING,
    /** Matches the query only when it is bounded by non-word characters or paragraph edges. */ WHOLE_WORD
}