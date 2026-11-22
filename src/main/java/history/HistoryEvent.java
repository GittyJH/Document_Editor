package history;

/** One chronological command-history event. */
public final class HistoryEvent {
    /** History event kind. */ public enum Type { EXECUTE, UNDO, REDO }
    private final Type type;
    private final String payload;

    /** Creates an event with an optional serialized command payload. */
    public HistoryEvent(Type type, String payload) {
        if (type == null) throw new IllegalArgumentException("History event type is required.");
        this.type = type;
        this.payload = payload == null ? "" : payload;
    }
    /** Returns the event type. */ public Type getType() { return type; }
    /** Returns the serialized command payload for execution events. */ public String getPayload() { return payload; }
}