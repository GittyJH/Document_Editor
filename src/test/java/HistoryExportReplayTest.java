import command.CommandProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Tests optional UC-09 portable history export and replay, including undo/redo and state restores. */
class HistoryExportReplayTest {
    @TempDir Path tempDir;

    @Test
    void exportedHistoryReconstructsCurrentStateAfterUndoRedoAndMacroReplay() throws Exception {
        CommandProcessor original = new CommandProcessor();
        original.execute("document create \"Replayable\"");
        original.execute("section add root Intro");
        original.execute("paragraph add 0 \"first draft\"");
        original.execute("checkpoint create start");
        original.execute("insert 0 5 \" revised\"");
        original.execute("undo");
        original.execute("redo");
        original.execute("macro start final-style");
        original.execute("format 0 0 5 bold true");
        original.execute("macro stop");
        original.execute("undo");
        original.execute("macro replay final-style");
        original.execute("checkpoint restore start");
        original.execute("redo");
        File history = tempDir.resolve("editor.history").toFile();
        original.getHistoryManager().exportHistory(history);

        CommandProcessor reconstructed = new CommandProcessor();
        java.util.concurrent.atomic.AtomicInteger changeNotifications = new java.util.concurrent.atomic.AtomicInteger();
        reconstructed.getDocument().addChangeListener(ignored -> changeNotifications.incrementAndGet());
        assertTrue(reconstructed.execute("history replay " + command.EditCommand.quote(history.getAbsolutePath())).startsWith("History replayed:"));
        assertEquals(1, changeNotifications.get());
        assertEquals(original.getDocument().getTitle(), reconstructed.getDocument().getTitle());
        assertEquals(original.getDocument().getStatistics().toString(), reconstructed.getDocument().getStatistics().toString());
        assertEquals(original.getDocument().getParagraph(0).getText(), reconstructed.getDocument().getParagraph(0).getText());
        assertEquals(original.execute("render markup 80 greedy"), reconstructed.execute("render markup 80 greedy"));
        assertEquals(original.getHistoryManager().getUndoDepth(), reconstructed.getHistoryManager().getUndoDepth());
        assertEquals(original.getHistoryManager().getRedoDepth(), reconstructed.getHistoryManager().getRedoDepth());
        assertEquals(original.getHistoryManager().getEvents().size(), reconstructed.getHistoryManager().getEvents().size());
        assertTrue(reconstructed.execute("undo").startsWith("Undone"));
        assertEquals("first revised draft", reconstructed.getDocument().getParagraph(0).getText());
        assertTrue(reconstructed.execute("render markup 80 greedy").contains("**first**"));
        assertTrue(reconstructed.execute("redo").startsWith("Redone"));
        assertEquals(original.getDocument().getParagraph(0).getText(), reconstructed.getDocument().getParagraph(0).getText());
    }


    @Test
    void replayRejectsTargetsWithMetadataOrPreviousHistoryEvenWithoutContent() throws Exception {
        var file = Files.createTempFile("editor-empty-target", ".history");
        Files.writeString(file, "DOCUMENT-EDITOR-HISTORY\t1\n");
        CommandProcessor titled = new CommandProcessor();
        titled.execute("document title \"Named\"");
        assertTrue(titled.execute("history replay " + file).startsWith("ERROR:"));

        CommandProcessor previouslyEdited = new CommandProcessor();
        previouslyEdited.execute("section add root Temporary");
        previouslyEdited.execute("undo");
        assertTrue(previouslyEdited.execute("history replay " + file).startsWith("ERROR:"));
    }

    @Test
    void malformedHistoryAndNonemptyTargetsAreRejectedWithoutMutation() throws Exception {
        Path bad = tempDir.resolve("bad.history");
        Files.writeString(bad, "not a document editor history");
        CommandProcessor empty = new CommandProcessor();
        assertTrue(empty.execute("history replay " + command.EditCommand.quote(bad.toString())).startsWith("ERROR:"));
        assertTrue(empty.getDocument().getSections().isEmpty());

        CommandProcessor populated = new CommandProcessor();
        populated.execute("section add root Existing");
        assertTrue(populated.execute("history replay " + command.EditCommand.quote(bad.toString())).startsWith("ERROR:"));
        assertEquals("Existing", populated.getDocument().getSection(0).getTitle());
    }

    @Test
    void historyFileFormatIsVersionedAndRejectsMalformedEventLines() {
        assertThrows(IllegalArgumentException.class,
                () -> history.HistoryFileCodec.decode("DOCUMENT-EDITOR-HISTORY\t99\n"));
        assertThrows(IllegalArgumentException.class,
                () -> history.HistoryFileCodec.decode("DOCUMENT-EDITOR-HISTORY\t1\nX\n"));
    }
}