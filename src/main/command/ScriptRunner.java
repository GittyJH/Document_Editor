package main.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Executes text command scripts in order through the same interface as interactive commands. */
public final class ScriptRunner {
    /** Executes every nonblank, non-comment line from a UTF-8 script file. */
    public List<String> run(Path script, CommandInterface commandInterface) throws IOException {
        if (script == null || commandInterface == null) throw new IllegalArgumentException("Script and command interface are required.");
        return runLines(Files.readAllLines(script, StandardCharsets.UTF_8), commandInterface);
    }

    /** Executes a supplied sequence of command lines in order. */
    public List<String> runLines(List<String> lines, CommandInterface commandInterface) {
        if (lines == null || commandInterface == null) throw new IllegalArgumentException("Lines and command interface are required.");
        List<String> outputs = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank() || line.stripLeading().startsWith("#")) continue;
            String result = commandInterface.execute(line);
            outputs.add(result);
            if (result.startsWith("ERROR:")) throw new IllegalArgumentException("Script failed on line " + (i + 1) + ": " + result);
        }
        return List.copyOf(outputs);
    }
}