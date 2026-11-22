package command;

import model.Document;
import service.StyleRegistry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Executes a sequence of commands as one atomic undoable document operation. */
public final class MacroCommand extends AbstractDocumentCommand {
    private final List<Command> commands;

    /** Creates a macro command from an ordered list of executable child commands. */
    public MacroCommand(Document document, StyleRegistry styles, List<Command> commands) {
        super(document, styles);
        if (commands == null || commands.isEmpty()) throw new IllegalArgumentException("A macro must contain at least one command.");
        this.commands = List.copyOf(commands);
    }

    @Override protected void perform() {
        document.beginChangeNotificationBatch();
        try {
            for (Command command : commands) command.execute();
        } finally {
            document.endChangeNotificationBatch();
        }
    }

    /** Returns the child commands in execution order. */ public List<Command> getCommands() { return commands; }
    @Override public String serialize() {
        StringBuilder result = new StringBuilder("@BATCH");
        for (Command command : commands) {
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(command.serialize().getBytes(StandardCharsets.UTF_8));
            result.append('|').append(encoded);
        }
        return result.toString();
    }
    @Override public String description() { return "Macro replay (" + commands.size() + " commands)"; }
}