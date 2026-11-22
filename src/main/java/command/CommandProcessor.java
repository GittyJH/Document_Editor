package command;

import history.HistoryManager;
import history.HistoryEvent;
import history.EditorStateCodec;
import history.HistoryReplayer;
import macro.MacroManager;
import model.Document;
import model.Match;
import model.MatchMode;
import model.Paragraph;
import render.GreedyLineBreak;
import render.HyphenationLineBreak;
import render.LineBreakStrategy;
import render.RenderMode;
import render.Renderer;
import service.SearchService;
import service.StatisticsService;
import service.StyleRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/** Coordinates parsing, document commands, history, macros, and presentation services. */
public final class CommandProcessor implements CommandInterface {
    private final Document document;
    private final StyleRegistry styles;
    private final HistoryManager historyManager;
    private final Renderer renderer;
    private final MacroManager macroManager;
    private final CommandParser parser;
    private final SearchService searchService;
    private final StatisticsService statisticsService;
    private final List<String> macroOutputs = new ArrayList<>();
    private boolean replayingMacro;

    /** Creates a processor with a new empty document. */
    public CommandProcessor() { this(new Document("Untitled"), new StyleRegistry()); }

    /** Creates a processor for a supplied document and style registry. */
    public CommandProcessor(Document document, StyleRegistry styles) {
        if (document == null || styles == null) throw new IllegalArgumentException("Document and style registry are required.");
        this.document = document;
        this.styles = styles;
        this.historyManager = new HistoryManager(document, styles);
        this.renderer = new Renderer(styles);
        this.macroManager = new MacroManager();
        this.parser = new CommandParser(document, styles, this::executeMacroAction);
        this.searchService = new SearchService();
        this.statisticsService = new StatisticsService();
    }

    /** Parses one document-changing command without executing it. */ public Command parse(String command) { return parser.parse(command); }
    /** Returns the active document. */ public Document getDocument() { return document; }
    /** Returns the processor's history manager. */ public HistoryManager getHistoryManager() { return historyManager; }
    /** Returns the processor's renderer. */ public Renderer getRenderer() { return renderer; }
    /** Returns the macro manager. */ public MacroManager getMacroManager() { return macroManager; }
    /** Returns the named-style registry. */ public StyleRegistry getStyleRegistry() { return styles; }

    /** Executes a command and converts validation or execution failures into concise error output. */
    @Override public String execute(String commandText) {
        if (commandText == null || commandText.isBlank()) return "";
        try {
            List<String> tokens = CommandParser.tokenize(commandText);
            if (tokens.isEmpty()) return "";
            String first = tokens.get(0).toLowerCase(Locale.ROOT);
            if (first.equals("help")) return help();
            if (first.equals("render")) {
                String output = render(tokens);
                recordAction(commandText);
                return output;
            }
            if (first.equals("stats")) {
                String output = statisticsService.calculate(document).toString();
                recordAction(commandText);
                return output;
            }
            if (first.equals("find")) {
                String output = find(tokens);
                recordAction(commandText);
                return output;
            }
            if (first.equals("undo")) { requireCount(tokens, 1, "undo"); historyManager.undo(); return "Undone."; }
            if (first.equals("redo")) { requireCount(tokens, 1, "redo"); historyManager.redo(); return "Redone."; }
            if (first.equals("checkpoint")) return checkpoint(tokens);
            if (first.equals("macro")) return macro(tokens);
            if (first.equals("history")) return history(tokens);
            Command command = parser.parse(commandText);
            executeCommand(command, !replayingMacro);
            return "OK: " + command.description();
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return "ERROR: " + safeMessage(exception);
        } catch (RuntimeException exception) {
            return "ERROR: " + safeMessage(exception);
        }
    }

    /** Executes a serialized command while reconstructing an exported history file. */
    public void executeSerializedCommand(String descriptor) {
        Command command = parser.parseSerialized(descriptor);
        command.execute();
        historyManager.record(command);
    }

    /** Applies one undo event while reconstructing an exported history file. */
    public void undoForReplay() { historyManager.undo(); }
    /** Applies one redo event while reconstructing an exported history file. */
    public void redoForReplay() { historyManager.redo(); }

    private void executeCommand(Command command, boolean recordForMacro) {
        command.execute();
        historyManager.record(command);
        if (recordForMacro && macroManager.isRecording()) macroManager.recordCommand(command.serialize());
    }

    private void recordAction(String sourceCommand) {
        if (macroManager.isRecording() && !replayingMacro) {
            macroManager.recordCommand(new ActionCommand(sourceCommand, () -> { }).serialize());
        }
    }

    private String executeMacroAction(String sourceCommand) {
        List<String> tokens = CommandParser.tokenize(sourceCommand);
        String result = switch (tokens.get(0).toLowerCase(Locale.ROOT)) {
            case "render" -> render(tokens);
            case "stats" -> statisticsService.calculate(document).toString();
            case "find" -> find(tokens);
            default -> throw new IllegalArgumentException("Unsupported non-mutating macro action: " + tokens.get(0));
        };
        if (result != null && !result.isEmpty()) macroOutputs.add(result);
        return result;
    }

    private String render(List<String> tokens) {
        requireCount(tokens, 4, "render <plain|markup> <width> <greedy|hyphenation>");
        RenderMode mode = switch (tokens.get(1).toLowerCase(Locale.ROOT)) {
            case "plain", "plain_text", "plain-text" -> RenderMode.PLAIN_TEXT;
            case "markup", "marked_up", "marked-up" -> RenderMode.MARKED_UP;
            default -> throw new IllegalArgumentException("Render mode must be plain or markup.");
        };
        int width = parseInteger(tokens.get(2), "line width");
        LineBreakStrategy strategy = switch (tokens.get(3).toLowerCase(Locale.ROOT)) {
            case "greedy" -> new GreedyLineBreak();
            case "hyphenation", "hyphen" -> new HyphenationLineBreak();
            default -> throw new IllegalArgumentException("Line-breaking method must be greedy or hyphenation.");
        };
        return renderer.render(document, mode, width, strategy);
    }

    private String find(List<String> tokens) {
        requireCount(tokens, 3, "find <substring|whole_word> <query>");
        MatchMode mode = switch (tokens.get(1).toLowerCase(Locale.ROOT)) {
            case "substring", "sub" -> MatchMode.SUBSTRING;
            case "whole_word", "whole-word", "word" -> MatchMode.WHOLE_WORD;
            default -> throw new IllegalArgumentException("Match mode must be substring or whole_word.");
        };
        List<Match> matches = searchService.find(document, tokens.get(2), mode);
        if (matches.isEmpty()) return "No matches found.";
        List<String> result = new ArrayList<>();
        for (Match match : matches) {
            int index = document.indexOf(match.getParagraph());
            result.add("paragraph " + index + " [" + match.getStartOffset() + "," + match.getEndOffset() + ")");
        }
        return String.join(System.lineSeparator(), result);
    }

    private String checkpoint(List<String> tokens) {
        if (tokens.size() != 3) throw new IllegalArgumentException("Syntax: checkpoint create <name> | checkpoint restore <name>");
        String operation = tokens.get(1).toLowerCase(Locale.ROOT);
        if (operation.equals("create")) {
            historyManager.createCheckpoint(tokens.get(2));
            return "Checkpoint saved: " + tokens.get(2);
        }
        if (operation.equals("restore")) {
            historyManager.restoreCheckpoint(tokens.get(2));
            if (macroManager.isRecording()) macroManager.recordCommand(historyManager.getEvents().get(historyManager.getEvents().size() - 1).getPayload());
            return "Checkpoint restored: " + tokens.get(2);
        }
        throw new IllegalArgumentException("Expected checkpoint create or checkpoint restore.");
    }

    private String macro(List<String> tokens) {
        if (tokens.size() < 2) throw new IllegalArgumentException("Syntax: macro start <name> | macro stop | macro replay <name> | macro list");
        return switch (tokens.get(1).toLowerCase(Locale.ROOT)) {
            case "start" -> {
                requireCount(tokens, 3, "macro start <name>");
                macroManager.startRecording(tokens.get(2));
                yield "Recording macro: " + tokens.get(2);
            }
            case "stop" -> {
                requireCount(tokens, 2, "macro stop");
                String name = macroManager.getRecordingName();
                macroManager.stopRecording();
                yield "Stopped recording macro: " + name;
            }
            case "list" -> {
                requireCount(tokens, 2, "macro list");
                yield macroManager.getMacroNames().isEmpty() ? "No macros saved." : String.join(System.lineSeparator(), macroManager.getMacroNames());
            }
            case "replay" -> {
                requireCount(tokens, 3, "macro replay <name>");
                if (replayingMacro) throw new IllegalStateException("Nested macro replay is not supported.");
                List<Command> children = new ArrayList<>();
                for (String descriptor : macroManager.getCommands(tokens.get(2))) children.add(parser.parseSerialized(descriptor));
                MacroCommand macro = new MacroCommand(document, styles, children);
                macroOutputs.clear();
                replayingMacro = true;
                try { executeCommand(macro, false); }
                finally { replayingMacro = false; }
                if (macroManager.isRecording()) macroManager.recordCommand(macro.serialize());
                String output = "Replayed macro: " + tokens.get(2);
                if (!macroOutputs.isEmpty()) output += System.lineSeparator() + String.join(System.lineSeparator(), macroOutputs);
                yield output;
            }
            default -> throw new IllegalArgumentException("Unknown macro operation: " + tokens.get(1));
        };
    }

    private String history(List<String> tokens) throws IOException {
        requireCount(tokens, 3, "history export <file> | history replay <file>");
        String operation = tokens.get(1).toLowerCase(Locale.ROOT);
        File file = new File(tokens.get(2));
        if (operation.equals("export")) {
            historyManager.exportHistory(file);
            return "History exported: " + file.getPath();
        }
        if (operation.equals("replay")) {
            if (!document.getTitle().equals("Untitled") || !document.getSections().isEmpty()
                    || !styles.getStyleNames().isEmpty() || !historyManager.isEmpty()) {
                throw new IllegalStateException("History replay requires a pristine empty document.");
            }
            HistoryReplayer.ReplayState validated = historyManager.replayHistoryState(file);
            var beforeDocument = document.snapshot();
            var beforeStyles = styles.snapshot();
            document.beginChangeNotificationBatch();
            try {
                for (HistoryEvent event : validated.getEvents()) {
                    switch (event.getType()) {
                        case EXECUTE -> executeSerializedCommand(event.getPayload());
                        case UNDO -> historyManager.undo();
                        case REDO -> historyManager.redo();
                    }
                }
                String expectedState = EditorStateCodec.encode(validated.getDocument().snapshot(), validated.getStyles());
                String actualState = EditorStateCodec.encode(document.snapshot(), styles.snapshot());
                if (!expectedState.equals(actualState)) {
                    throw new IllegalStateException("Replayed history did not reproduce its validated document state.");
                }
            } catch (RuntimeException exception) {
                document.restoreFrom(beforeDocument);
                styles.restore(beforeStyles);
                historyManager.clearHistory();
                throw new IllegalStateException("History replay failed safely: " + safeMessage(exception), exception);
            } finally {
                document.endChangeNotificationBatch();
            }
            if (!validated.getEvents().isEmpty()) document.publishChange();
            return "History replayed: " + file.getPath();
        }
        throw new IllegalArgumentException("Expected history export or history replay.");
    }

    private static int parseInteger(String text, String label) {
        try { return Integer.parseInt(text); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid " + label + ": " + text); }
    }

    private static void requireCount(List<String> tokens, int count, String syntax) {
        if (tokens.size() != count) throw new IllegalArgumentException("Syntax: " + syntax);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String help() {
        return "Document Editor commands:\n"
                + "  document create|title <title>\n"
                + "  section add <root|path> <title> | section remove <path>\n"
                + "  paragraph add <sectionPath> <text>\n"
                + "  insert <paragraph> <offset> <text> | delete <paragraph> <start> <end>\n"
                + "  edit-replace <paragraph> <start> <end> <text> | move <paragraph> <start> <end> <destination>\n"
                + "  split <paragraph> <offset> | merge <paragraph>\n"
                + "  format <paragraph> <start> <end> <property> <value|inherit>\n"
                + "  format-title <sectionPath> <property> <value|inherit>\n"
                + "  style define <name> <property> <value> | style apply <sectionPath> <name>\n"
                + "  find <substring|whole_word> <query> | replace-first|replace-all <mode> <query> <replacement>\n"
                + "  undo | redo | checkpoint create|restore <name>\n"
                + "  render <plain|markup> <width> <greedy|hyphenation> | stats\n"
                + "  macro start|stop|list|replay <name> | history export|replay <file>";
    }
}