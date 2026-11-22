package macro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns named macros and records successful commands while recording is active. */
public final class MacroManager {
    private final Map<String, Macro> macros = new LinkedHashMap<>();
    private Macro recording;

    /** Starts recording a new macro, replacing any existing macro with the same name. */
    public Macro startRecording(String name) {
        if (recording != null) throw new IllegalStateException("A macro is already being recorded.");
        recording = new Macro(name);
        return recording;
    }

    /** Stops the active recording and stores the named macro. */
    public void stopRecording() {
        if (recording == null) throw new IllegalStateException("No macro is currently being recorded.");
        Macro completed = recording;
        recording = null;
        if (completed.isEmpty()) throw new IllegalStateException("Cannot save an empty macro.");
        macros.put(completed.getName(), completed);
    }

    /** Records a successful command when a macro is currently active. */
    public void recordCommand(String commandDescriptor) {
        if (recording != null) recording.addCommand(commandDescriptor);
    }

    /** Returns the recorded command descriptors for the named macro. */
    public List<String> getCommands(String name) {
        Macro macro = macros.get(name);
        if (macro == null) throw new IllegalArgumentException("Unknown macro: " + name);
        return macro.getCommands();
    }

    /** Returns the command sequence as newline-separated text. */
    public String replay(String name) { return String.join("\n", getCommands(name)); }
    /** Returns all saved macro names in insertion order. */ public List<String> getMacroNames() { return List.copyOf(macros.keySet()); }
    /** Returns whether a macro is currently being recorded. */ public boolean isRecording() { return recording != null; }
    /** Returns the current macro name, or {@code null} when recording is inactive. */ public String getRecordingName() { return recording == null ? null : recording.getName(); }
}