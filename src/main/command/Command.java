package main.command;

/** Encapsulates one reversible document operation. */
public interface Command {
    /** Executes the operation. */
    void execute();
    /** Reverses a previously executed operation. */
    void undo();
    /** Reapplies an operation that has been undone. */
    default void redo() { execute(); }
    /** Returns a versioned, implementation-independent description for history export. */
    String serialize();
    /** Returns a short human-readable command description. */
    String description();
}