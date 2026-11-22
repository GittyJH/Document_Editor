import command.CommandProcessor;
import command.TextCommandInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

/** Application entry point for interactive or scripted command execution. */
public final class Main {
    private Main() { }

    /** Starts an interactive editor, or runs a script supplied as the first argument. */
    public static void main(String[] args) throws IOException {
        CommandProcessor processor = new CommandProcessor();
        TextCommandInterface commandInterface = new TextCommandInterface(processor);
        if (args.length > 0) {
            try {
                for (String output : new command.ScriptRunner().run(Path.of(args[0]), commandInterface)) {
                    if (!output.isEmpty()) System.out.println(output);
                }
            } catch (RuntimeException exception) {
                System.err.println(exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("Document Editor. Type 'help' for commands or 'quit' to exit.");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = input.readLine();
            if (line == null || line.trim().equalsIgnoreCase("quit")) break;
            String output = commandInterface.execute(line);
            if (!output.isEmpty()) System.out.println(output);
        }
    }
}