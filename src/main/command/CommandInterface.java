package main.command;

/** User-facing interface for executing complete text commands. */
public interface CommandInterface {
    /** Executes one complete command line and returns its textual result. */
    String execute(String command);
}