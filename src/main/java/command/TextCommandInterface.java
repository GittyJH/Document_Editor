package command;

/** User-facing adapter around the command processor. */
public final class TextCommandInterface implements CommandInterface {
    private final CommandProcessor processor;

    /** Creates an interface for the supplied processor. */
    public TextCommandInterface(CommandProcessor processor) {
        if (processor == null) throw new IllegalArgumentException("Command processor cannot be null.");
        this.processor = processor;
    }

    /** Executes one complete text command and returns its output. */
    @Override public String execute(String command) { return processor.execute(command); }
    /** Returns the backing command processor. */ public CommandProcessor getProcessor() { return processor; }
}