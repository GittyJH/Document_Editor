package command;

import history.EditorStateCodec;
import model.Document;
import model.FormatProperty;
import model.Match;
import model.MatchMode;
import service.SearchService;
import service.StyleRegistry;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/** Converts complete text commands into independent command objects. */
public final class CommandParser {
    private final Document document;
    private final StyleRegistry styles;
    private final SearchService search;
    private final Function<String, String> actionExecutor;

    /** Creates a parser bound to the document and style registry it will command. */
    public CommandParser(Document document, StyleRegistry styles) {
        this(document, styles, ignored -> "");
    }

    /** Creates a parser with an executor for non-mutating macro actions. */
    public CommandParser(Document document, StyleRegistry styles, Function<String, String> actionExecutor) {
        this.document = document;
        this.styles = styles;
        this.search = new SearchService();
        this.actionExecutor = actionExecutor == null ? ignored -> "" : actionExecutor;
    }

    /** Parses one supported document-changing command without executing it. */
    public Command parse(String commandText) {
        List<String> tokens = tokenize(commandText);
        if (tokens.isEmpty()) throw new IllegalArgumentException("Command cannot be empty.");
        String command = tokens.get(0).toLowerCase(Locale.ROOT);
        if (command.equals("render") || command.equals("stats") || command.equals("find")) {
            return new ActionCommand(commandText, () -> actionExecutor.apply(commandText));
        }
        if (command.equals("document")) {
            require(tokens, 3, "document create <title> | document title <title>");
            return switch (tokens.get(1).toLowerCase(Locale.ROOT)) {
                case "create" -> new StructureCommand(document, styles, StructureCommand.Kind.CREATE_DOCUMENT, null, tokens.get(2));
                case "title" -> new StructureCommand(document, styles, StructureCommand.Kind.SET_TITLE, null, tokens.get(2));
                default -> throw new IllegalArgumentException("Expected 'document create' or 'document title'.");
            };
        }
        if (command.equals("section")) {
            if (tokens.size() < 3) throw syntax("section add <root|path> <title> | section remove <path>");
            String operation = tokens.get(1).toLowerCase(Locale.ROOT);
            if (operation.equals("add")) {
                require(tokens, 4, "section add <root|path> <title>");
                return new StructureCommand(document, styles, StructureCommand.Kind.ADD_SECTION, tokens.get(2), tokens.get(3));
            }
            if (operation.equals("remove")) {
                require(tokens, 3, "section remove <path>");
                return new StructureCommand(document, styles, StructureCommand.Kind.REMOVE_SECTION, tokens.get(2), "");
            }
            throw new IllegalArgumentException("Unknown section operation: " + operation);
        }
        if (command.equals("paragraph")) {
            require(tokens, 4, "paragraph add <sectionPath> <text>");
            if (!tokens.get(1).equalsIgnoreCase("add")) throw new IllegalArgumentException("Expected 'paragraph add'.");
            return new StructureCommand(document, styles, StructureCommand.Kind.ADD_PARAGRAPH, tokens.get(2), tokens.get(3));
        }
        if (command.equals("insert")) {
            require(tokens, 4, "insert <paragraphIndex> <offset> <text>");
            return EditCommand.insert(document, styles, integer(tokens.get(1), "paragraph index"), integer(tokens.get(2), "offset"), tokens.get(3));
        }
        if (command.equals("delete")) {
            require(tokens, 4, "delete <paragraphIndex> <start> <end>");
            return EditCommand.delete(document, styles, integer(tokens.get(1), "paragraph index"),
                    integer(tokens.get(2), "start"), integer(tokens.get(3), "end"));
        }
        if (command.equals("edit-replace") || command.equals("replace-range")) {
            require(tokens, 5, "edit-replace <paragraphIndex> <start> <end> <text>");
            return EditCommand.replace(document, styles, integer(tokens.get(1), "paragraph index"),
                    integer(tokens.get(2), "start"), integer(tokens.get(3), "end"), tokens.get(4));
        }
        if (command.equals("move")) {
            require(tokens, 5, "move <paragraphIndex> <start> <end> <destination>");
            return EditCommand.move(document, styles, integer(tokens.get(1), "paragraph index"),
                    integer(tokens.get(2), "start"), integer(tokens.get(3), "end"), integer(tokens.get(4), "destination"));
        }
        if (command.equals("split")) {
            require(tokens, 3, "split <paragraphIndex> <offset>");
            return EditCommand.split(document, styles, integer(tokens.get(1), "paragraph index"), integer(tokens.get(2), "offset"));
        }
        if (command.equals("merge")) {
            require(tokens, 2, "merge <paragraphIndex>");
            return EditCommand.merge(document, styles, integer(tokens.get(1), "paragraph index"));
        }
        if (command.equals("format")) {
            require(tokens, 6, "format <paragraphIndex> <start> <end> <property> <value|inherit>");
            FormatProperty property = FormatProperty.fromString(tokens.get(4));
            boolean clear = tokens.get(5).equalsIgnoreCase("inherit");
            return new FormatCommand(document, styles, integer(tokens.get(1), "paragraph index"),
                    integer(tokens.get(2), "start"), integer(tokens.get(3), "end"), property, tokens.get(5), clear);
        }
        if (command.equals("format-title")) {
            require(tokens, 4, "format-title <sectionPath> <property> <value|inherit>");
            FormatProperty property = FormatProperty.fromString(tokens.get(2));
            boolean clear = tokens.get(3).equalsIgnoreCase("inherit");
            return new FormatCommand(document, styles, tokens.get(1), property, tokens.get(3), clear);
        }
        if (command.equals("style")) {
            requireAtLeast(tokens, 4, "style define <name> <property> <value> | style apply <sectionPath> <name>");
            return switch (tokens.get(1).toLowerCase(Locale.ROOT)) {
                case "define" -> {
                    require(tokens, 5, "style define <name> <property> <value>");
                    yield new StyleCommand(document, styles, tokens.get(2), FormatProperty.fromString(tokens.get(3)), tokens.get(4));
                }
                case "apply" -> {
                    require(tokens, 4, "style apply <sectionPath> <name>");
                    yield new StyleCommand(document, styles, tokens.get(2), tokens.get(3));
                }
                default -> throw new IllegalArgumentException("Expected 'style define' or 'style apply'.");
            };
        }
        if (command.equals("replace-first") || command.equals("replace-all")) {
            require(tokens, 4, command + " <substring|whole_word> <query> <replacement>");
            MatchMode mode = parseMatchMode(tokens.get(1));
            List<Match> matches = search.find(document, tokens.get(2), mode);
            if (matches.isEmpty()) throw new IllegalArgumentException("No matching text was found.");
            boolean all = command.equals("replace-all");
            if (!all) matches = List.of(matches.get(0));
            return new ReplaceCommand(document, styles, matches, tokens.get(2), mode, tokens.get(3), all);
        }
        throw new IllegalArgumentException("Unknown document-changing command: " + tokens.get(0));
    }

    /** Parses an exported command descriptor, including batched macro and state records. */
    public Command parseSerialized(String descriptor) {
        if (descriptor == null || descriptor.isBlank()) throw new IllegalArgumentException("Serialized command cannot be empty.");
        if (descriptor.startsWith("@STATE|")) {
            return new StateRestoreCommand(document, styles, EditorStateCodec.decode(descriptor));
        }
        if (descriptor.startsWith("@ACTION|")) {
            try {
                String source = new String(Base64.getUrlDecoder().decode(descriptor.substring(8)), StandardCharsets.UTF_8);
                return new ActionCommand(source, () -> actionExecutor.apply(source));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid serialized action command.", exception);
            }
        }
        if (descriptor.startsWith("@BATCH")) {
            String[] pieces = descriptor.split("\\|", -1);
            if (pieces.length < 2) throw new IllegalArgumentException("Serialized macro is empty.");
            List<Command> commands = new ArrayList<>();
            for (int i = 1; i < pieces.length; i++) {
                try {
                    String child = new String(Base64.getUrlDecoder().decode(pieces[i]), StandardCharsets.UTF_8);
                    commands.add(parseSerialized(child));
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Invalid serialized macro command.", exception);
                }
            }
            return new MacroCommand(document, styles, commands);
        }
        return parse(descriptor);
    }

    /** Tokenizes one command line, honoring double quotes and backslash escapes. */
    public static List<String> tokenize(String commandText) {
        if (commandText == null) throw new IllegalArgumentException("Command cannot be null.");
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        boolean escaping = false;
        boolean tokenStarted = false;
        for (int i = 0; i < commandText.length(); i++) {
            char character = commandText.charAt(i);
            if (escaping) {
                token.append(character);
                escaping = false;
                tokenStarted = true;
            } else if (character == '\\') {
                escaping = true;
                tokenStarted = true;
            } else if (character == '"') {
                quoted = !quoted;
                tokenStarted = true;
            } else if (Character.isWhitespace(character) && !quoted) {
                if (tokenStarted) {
                    tokens.add(token.toString());
                    token.setLength(0);
                    tokenStarted = false;
                }
            } else {
                token.append(character);
                tokenStarted = true;
            }
        }
        if (escaping) token.append('\\');
        if (quoted) throw new IllegalArgumentException("Unterminated quoted argument.");
        if (tokenStarted) tokens.add(token.toString());
        return List.copyOf(tokens);
    }

    private static MatchMode parseMatchMode(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "substring", "sub" -> MatchMode.SUBSTRING;
            case "whole_word", "whole-word", "word" -> MatchMode.WHOLE_WORD;
            default -> throw new IllegalArgumentException("Match mode must be substring or whole_word.");
        };
    }

    private static int integer(String value, String label) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid " + label + ": " + value); }
    }
    private static void require(List<String> tokens, int count, String syntax) {
        if (tokens.size() != count) throw syntax(syntax);
    }
    private static void requireAtLeast(List<String> tokens, int count, String syntax) {
        if (tokens.size() < count) throw syntax(syntax);
    }
    private static IllegalArgumentException syntax(String syntax) { return new IllegalArgumentException("Syntax: " + syntax); }
}