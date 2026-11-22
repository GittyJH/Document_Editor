package macro;

import java.util.ArrayList;
import java.util.List;

/** A named reusable sequence of command descriptors. */
public final class Macro {
    private final String name;
    private final List<String> commands = new ArrayList<>();

    /** Creates a macro with a nonblank name. */
    public Macro(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Macro name cannot be empty.");
        this.name = name.trim();
    }

    /** Returns the macro name. */ public String getName() { return name; }
    /** Adds a command descriptor to the end of the macro. */
    public void addCommand(String command) {
        if (command == null || command.isBlank()) throw new IllegalArgumentException("Macro command cannot be empty.");
        commands.add(command);
    }
    /** Returns an immutable list of command descriptors in recording order. */ public List<String> getCommands() { return List.copyOf(commands); }
    /** Returns whether this macro contains any commands. */ public boolean isEmpty() { return commands.isEmpty(); }
}