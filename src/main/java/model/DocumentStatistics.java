package model;

/** Immutable word, character, and paragraph counts for a document state. */
public final class DocumentStatistics {
    private final int wordCnt;
    private final int charCnt;
    private final int paraCnt;

    /** Creates a statistics value. */
    public DocumentStatistics(int wordCnt, int charCnt, int paraCnt) {
        this.wordCnt = wordCnt;
        this.charCnt = charCnt;
        this.paraCnt = paraCnt;
    }

    /** Returns the number of words in section titles and paragraph text. */ public int getWordCnt() { return wordCnt; }
    /** Returns the number of characters in section titles and paragraph text. */ public int getCharCnt() { return charCnt; }
    /** Returns the number of paragraphs throughout the nested document. */ public int getParaCnt() { return paraCnt; }

    /** Returns a display for the text command interface. */
    @Override public String toString() { return "Words: " + wordCnt + ", Characters: " + charCnt + ", Paragraphs: " + paraCnt; }
}