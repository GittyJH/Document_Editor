package model;

import java.util.ArrayList;
import java.util.List;

/** Immutable deep snapshot used by undo/redo commands and named checkpoints. */
public final class DocumentSnapshot {
    private final String title;
    private final List<Section> sections;

    /** Captures a deep copy of the document's current structured content. */
    public DocumentSnapshot(Document document) {
        this.title = document.getTitle();
        this.sections = new ArrayList<>();
        for (Section section : document.getSections()) sections.add(section.deepCopy());
    }

    /** Returns an independent mutable document reconstructed from this snapshot. */
    public Document toDocument() {
        Document document = new Document(title);
        for (Section section : sections) document.addSection(section.deepCopy());
        return document;
    }

    /** Restores this snapshot into the supplied document instance. */
    public void restore(Document document) {
        document.restoreFrom(this);
    }

    /** Returns a defensive copy of the snapshot's root sections. */
    public List<Section> getSections() {
        List<Section> result = new ArrayList<>();
        for (Section section : sections) result.add(section.deepCopy());
        return List.copyOf(result);
    }

    /** Returns the document title captured by this snapshot. */ public String getTitle() { return title; }
}