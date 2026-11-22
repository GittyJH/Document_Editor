package main.actor;

import main.command.CommandInterface;

/** Base actor that communicates with the editor through the text command interface. */
public abstract class User {
    private final String id;
    private final CommandInterface commandInterface;
    private String lastOutput = "";

    /** Creates a user actor with a nonblank identifier and command interface. */
    protected User(String id, CommandInterface commandInterface) {
        if (id == null || id.isBlank() || commandInterface == null) throw new IllegalArgumentException("User id and command interface are required.");
        this.id = id;
        this.commandInterface = commandInterface;
    }

    /** Returns the user identifier. */ public String getId() { return id; }
    /** Sends one command to the editor and retains its output. */
    public String enterCommand(String command) {
        lastOutput = commandInterface.execute(command);
        return lastOutput;
    }
    /** Displays the most recent command output on standard output. */
    public void viewOutput() { System.out.println(lastOutput); }
    /** Returns the most recent command output. */ public String getLastOutput() { return lastOutput; }
}