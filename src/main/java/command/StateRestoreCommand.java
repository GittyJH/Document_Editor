package command;

import history.EditorStateCodec;
import model.Document;
import service.StyleRegistry;

/** Restores a complete serialized document/style state as one reversible command. */
public final class StateRestoreCommand extends AbstractDocumentCommand {
    private final EditorStateCodec.DecodedState state;

    /** Creates a command that restores the decoded state when executed. */
    public StateRestoreCommand(Document document, StyleRegistry styles, EditorStateCodec.DecodedState state) {
        super(document, styles);
        this.state = state;
    }

    @Override protected void perform() {
        state.getDocument().restore(document);
        styles.restore(state.getStyles());
    }

    @Override public String serialize() {
        return EditorStateCodec.encode(state.getDocument(), state.getStyles());
    }
    @Override public String description() { return "Restore document state"; }
}