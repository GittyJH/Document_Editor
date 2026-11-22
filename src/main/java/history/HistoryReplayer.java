package history;

import command.CommandProcessor;
import model.Document;
import service.StyleRegistry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Reconstructs document state by replaying portable execute/undo/redo events. */
public final class HistoryReplayer {
    private final FileSystem fileSystem;

    /** Creates a history replayer backed by the local file system. */
    public HistoryReplayer() { this(new LocalFileSystem()); }
    /** Creates a history replayer with an injectable file-system abstraction. */
    public HistoryReplayer(FileSystem fileSystem) { this.fileSystem = fileSystem; }

    /** Replays a history file into a new document and returns the complete editor state. */
    public ReplayState replay(File file) throws IOException {
        List<HistoryEvent> events;
        try { events = HistoryFileCodec.decode(fileSystem.read(file)); }
        catch (IllegalArgumentException exception) { throw new IOException(exception.getMessage(), exception); }
        Document document = new Document("Untitled");
        StyleRegistry styles = new StyleRegistry();
        CommandProcessor processor = new CommandProcessor(document, styles);
        try {
            for (HistoryEvent event : events) {
                switch (event.getType()) {
                    case EXECUTE -> processor.executeSerializedCommand(event.getPayload());
                    case UNDO -> processor.undoForReplay();
                    case REDO -> processor.redoForReplay();
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("History replay failed: " + exception.getMessage(), exception);
        }
        return new ReplayState(document, styles, events);
    }

    /** Complete reconstructed document and inherited-style state. */
    public static final class ReplayState {
        private final Document document;
        private final StyleRegistry.State styles;
        private final List<HistoryEvent> events;
        private ReplayState(Document document, StyleRegistry styles, List<HistoryEvent> events) {
            this.document = document.deepCopy();
            this.styles = styles.snapshot();
            this.events = List.copyOf(events);
        }
        /** Returns an independent copy of the reconstructed document. */ public Document getDocument() { return document.deepCopy(); }
        /** Returns the reconstructed style-registry snapshot. */ public StyleRegistry.State getStyles() { return styles; }
        /** Returns the validated chronological events used to reconstruct this state. */ public List<HistoryEvent> getEvents() { return events; }
    }
}