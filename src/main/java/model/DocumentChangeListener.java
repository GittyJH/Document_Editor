package model;

/** Observer notified after a document-changing command succeeds, is undone, or is redone. */
@FunctionalInterface
public interface DocumentChangeListener {
    /** Receives the document whose content has just changed. */
    void documentChanged(Document document);
}