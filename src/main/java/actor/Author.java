package actor;

import command.CommandInterface;
import command.EditCommand;

/** Primary actor responsible for creating, editing, and managing a document. */
public final class Author extends User {
    private final String name;

    /** Creates an author with a name, actor id, and command interface. */
    public Author(String id, String name, CommandInterface commandInterface) {
        super(id, commandInterface);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Author name cannot be empty.");
        this.name = name;
    }

    /** Returns the author's display name. */ public String getName() { return name; }
    /** Creates or resets the active document with the supplied title. */
    public String createDocument(String title) { return enterCommand("document create " + EditCommand.quote(title)); }
    /** Executes one document-editing command. */ public String editDocument(String command) { return enterCommand(command); }
    /** Executes an undo, redo, checkpoint, or history command. */ public String manageHistory(String command) { return enterCommand(command); }
}