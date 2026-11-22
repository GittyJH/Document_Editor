package history;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Encodes chronological history events in a portable, versioned text format. */
public final class HistoryFileCodec {
    private static final String HEADER = "DOCUMENT-EDITOR-HISTORY\t1";
    private HistoryFileCodec() { }

    /** Encodes history events as a versioned UTF-8 text document. */
    public static String encode(List<HistoryEvent> events) {
        StringBuilder result = new StringBuilder(HEADER).append('\n');
        for (HistoryEvent event : events) {
            if (event.getType() == HistoryEvent.Type.EXECUTE) {
                String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(event.getPayload().getBytes(StandardCharsets.UTF_8));
                result.append("E\t").append(payload).append('\n');
            } else if (event.getType() == HistoryEvent.Type.UNDO) result.append("U\n");
            else result.append("R\n");
        }
        return result.toString();
    }

    /** Decodes a history file, rejecting unsupported versions and malformed events. */
    public static List<HistoryEvent> decode(String data) {
        if (data == null) throw new IllegalArgumentException("History content cannot be null.");
        String[] lines = data.split("\\R", -1);
        if (lines.length == 0 || !HEADER.equals(lines[0])) throw new IllegalArgumentException("Invalid or unsupported history file.");
        List<HistoryEvent> events = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;
            if (line.equals("U")) events.add(new HistoryEvent(HistoryEvent.Type.UNDO, ""));
            else if (line.equals("R")) events.add(new HistoryEvent(HistoryEvent.Type.REDO, ""));
            else if (line.startsWith("E\t")) {
                try {
                    String payload = new String(Base64.getUrlDecoder().decode(line.substring(2)), StandardCharsets.UTF_8);
                    if (payload.isBlank()) throw new IllegalArgumentException();
                    events.add(new HistoryEvent(HistoryEvent.Type.EXECUTE, payload));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Invalid command payload on history line " + (i + 1) + ".");
                }
            } else throw new IllegalArgumentException("Invalid history event on line " + (i + 1) + ".");
        }
        return List.copyOf(events);
    }
}