package main.command;

/** Identifies the text or paragraph-organization operation performed by an edit command. */
public enum EditOperation {
    /** Insert text at a position. */ INSERT,
    /** Delete a range of text. */ DELETE,
    /** Replace a range of text. */ REPLACE,
    /** Move a range within one paragraph. */ MOVE,
    /** Split a paragraph at a position. */ SPLIT,
    /** Merge a paragraph with its immediate following paragraph. */ MERGE
}