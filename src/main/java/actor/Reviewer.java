package actor;

import command.CommandInterface;
import command.EditCommand;

/** Secondary actor that inspects the current document and restores named checkpoints. */
public final class Reviewer extends User {
    private final String name;

    /** Creates a reviewer with a name, actor id, and command interface. */
    public Reviewer(String id, String name, CommandInterface commandInterface) {
        super(id, commandInterface);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Reviewer name cannot be empty.");
        this.name = name;
    }

    /** Returns the reviewer's display name. */ public String getName() { return name; }
    /** Renders the current document as plain text at the requested width. */
    public String viewDocument(int width) { return enterCommand("render plain " + width + " greedy"); }
    /** Restores a named checkpoint for review. */
    public String restoreCheckpoint(String name) { return enterCommand("checkpoint restore " + EditCommand.quote(name)); }
}