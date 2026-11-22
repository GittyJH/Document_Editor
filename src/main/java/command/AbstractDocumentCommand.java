package command;

import model.Document;
import model.DocumentSnapshot;
import service.StyleRegistry;

/** Base class implementing atomic execution and snapshot-backed undo/redo. */
public abstract class AbstractDocumentCommand implements Command {
    /** Document affected by this command. */
    protected final Document document;
    /** Inherited-style registry affected by this command, when applicable. */
    protected final StyleRegistry styles;
    private DocumentSnapshot before;
    private DocumentSnapshot after;
    private StyleRegistry.State stylesBefore;
    private StyleRegistry.State stylesAfter;
    private boolean executed;
    private boolean undone;

    /** Creates a document command with optional inherited-style support. */
    protected AbstractDocumentCommand(Document document, StyleRegistry styles) {
        if (document == null) throw new IllegalArgumentException("Document cannot be null.");
        this.document = document;
        this.styles = styles;
    }

    /** Performs the concrete mutation after validation. */
    protected abstract void perform();

    @Override public final void execute() {
        before = document.snapshot();
        stylesBefore = styles == null ? null : styles.snapshot();
        try {
            perform();
            after = document.snapshot();
            stylesAfter = styles == null ? null : styles.snapshot();
            executed = true;
            undone = false;
            document.publishChange();
        } catch (RuntimeException exception) {
            before.restore(document);
            if (styles != null && stylesBefore != null) styles.restore(stylesBefore);
            before = null;
            after = null;
            throw exception;
        }
    }

    @Override public final void undo() {
        if (!executed || undone || before == null) throw new IllegalStateException("Command cannot be undone in its current state.");
        before.restore(document);
        if (styles != null && stylesBefore != null) styles.restore(stylesBefore);
        undone = true;
        document.publishChange();
    }

    @Override public final void redo() {
        if (!executed || !undone || after == null) throw new IllegalStateException("Command cannot be redone in its current state.");
        after.restore(document);
        if (styles != null && stylesAfter != null) styles.restore(stylesAfter);
        undone = false;
        document.publishChange();
    }
}