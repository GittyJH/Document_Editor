package main.command;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A non-mutating command action included in a recorded macro's execution sequence. */
public final class ActionCommand implements Command {
    private final String sourceCommand;
    private final Runnable action;

    /** Creates an action command from its original text and an execution callback. */
    public ActionCommand(String sourceCommand, Runnable action) {
        if (sourceCommand == null || sourceCommand.isBlank() || action == null) throw new IllegalArgumentException("Action command and callback are required.");
        this.sourceCommand = sourceCommand;
        this.action = action;
    }

    /** Executes the non-mutating action. */ @Override public void execute() { action.run(); }
    /** Does nothing because this action does not modify document state. */ @Override public void undo() { }
    /** Repeats the non-mutating action. */ @Override public void redo() { action.run(); }
    /** Returns a portable descriptor preserving the original command text. */
    @Override public String serialize() {
        return "@ACTION|" + Base64.getUrlEncoder().withoutPadding().encodeToString(sourceCommand.getBytes(StandardCharsets.UTF_8));
    }
    /** Returns a description of the action. */ @Override public String description() { return sourceCommand; }
}