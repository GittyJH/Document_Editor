package history;

import command.Command;
import command.StateRestoreCommand;
import model.Document;
import model.DocumentSnapshot;
import service.StyleRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maintains unbounded undo/redo stacks, named checkpoints, and an exportable event log. */
public final class HistoryManager {
    private final Document document;
    private final StyleRegistry styles;
    private final FileSystem fileSystem;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final Map<String, Checkpoint> checkpoints = new LinkedHashMap<>();
    private final List<HistoryEvent> events = new ArrayList<>();

    /** Creates a history manager for one document. */
    public HistoryManager(Document document, StyleRegistry styles) { this(document, styles, new LocalFileSystem()); }

    /** Creates a history manager with an injectable file-system implementation. */
    public HistoryManager(Document document, StyleRegistry styles, FileSystem fileSystem) {
        this.document = document;
        this.styles = styles;
        this.fileSystem = fileSystem;
    }

    /** Records a successfully executed command and discards the redo branch. */
    public void record(Command command) {
        if (command == null) throw new IllegalArgumentException("Command cannot be null.");
        undoStack.push(command);
        redoStack.clear();
        events.add(new HistoryEvent(HistoryEvent.Type.EXECUTE, command.serialize()));
    }

    /** Undoes the most recent command. */
    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo.");
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        events.add(new HistoryEvent(HistoryEvent.Type.UNDO, ""));
    }

    /** Redoes the most recently undone command. */
    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo.");
        Command command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        events.add(new HistoryEvent(HistoryEvent.Type.REDO, ""));
    }

    /** Saves the complete document and style-registry state under a checkpoint name. */
    public void createCheckpoint(String name) {
        validateName(name, "Checkpoint");
        checkpoints.put(name, new Checkpoint(document.snapshot(), styles.snapshot()));
    }

    /** Restores a named checkpoint as one undoable operation. */
    public void restoreCheckpoint(String name) {
        Checkpoint checkpoint = checkpoints.get(name);
        if (checkpoint == null) throw new IllegalArgumentException("Unknown checkpoint: " + name);
        EditorStateCodec.DecodedState state = EditorStateCodec.decode(
                EditorStateCodec.encode(checkpoint.document, checkpoint.styles));
        Command command = new StateRestoreCommand(document, styles, state);
        command.execute();
        record(command);
    }

    /** Returns the number of commands currently available to undo. */ public int getUndoDepth() { return undoStack.size(); }
    /** Returns the number of commands currently available to redo. */ public int getRedoDepth() { return redoStack.size(); }
    /** Returns whether the manager has no history events, undo/redo entries, or saved checkpoints. */
    public boolean isEmpty() { return undoStack.isEmpty() && redoStack.isEmpty() && events.isEmpty() && checkpoints.isEmpty(); }
    /** Returns an immutable copy of the chronological history event log. */ public List<HistoryEvent> getEvents() { return List.copyOf(events); }

    /** Exports the complete execute/undo/redo event sequence to a portable history file. */
    public void exportHistory(File file) throws IOException { fileSystem.write(file, HistoryFileCodec.encode(events)); }

    /** Replays an exported history file from a new empty document and returns the reconstructed document. */
    public Document replayHistory(File file) throws IOException { return new HistoryReplayer(fileSystem).replay(file).getDocument(); }

    /** Replays a history file and returns both document and inherited-style state. */
    public HistoryReplayer.ReplayState replayHistoryState(File file) throws IOException {
        return new HistoryReplayer(fileSystem).replay(file);
    }

    /** Clears undo, redo, checkpoints, and export events after a successful history reconstruction. */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        checkpoints.clear();
        events.clear();
    }

    private static void validateName(String name, String type) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException(type + " name cannot be empty.");
    }

    private static final class Checkpoint {
        private final DocumentSnapshot document;
        private final StyleRegistry.State styles;
        private Checkpoint(DocumentSnapshot document, StyleRegistry.State styles) {
            this.document = document;
            this.styles = styles;
        }
    }
}