import command.CommandProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unbounded command history, branch invalidation, named checkpoints, replace-all, and macros. */
class HistoryAndMacroTest {
    @Test
    void undoRedoAndNewEditBranchingWork() {
        CommandProcessor processor = fixture("a");
        processor.execute("insert 0 1 \"b\"");
        processor.execute("insert 0 2 \"c\"");
        assertEquals("abc", processor.getDocument().getParagraph(0).getText());
        assertEquals("Undone.", processor.execute("undo"));
        assertEquals("ab", processor.getDocument().getParagraph(0).getText());
        assertEquals("Redone.", processor.execute("redo"));
        assertEquals("abc", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        processor.execute("insert 0 2 \"X\"");
        assertEquals("abX", processor.getDocument().getParagraph(0).getText());
        assertTrue(processor.execute("redo").startsWith("ERROR:"));
    }

    @Test
    void historyHasNoSmallFixedDepthLimit() {
        CommandProcessor processor = fixture("");
        for (int i = 0; i < 125; i++) processor.execute("insert 0 " + i + " \"x\"");
        assertEquals(127, processor.getHistoryManager().getUndoDepth());
        for (int i = 0; i < 125; i++) processor.execute("undo");
        assertEquals("", processor.getDocument().getParagraph(0).getText());
        assertEquals(125, processor.getHistoryManager().getRedoDepth());
    }

    @Test
    void checkpointRestoresCompleteDocumentAndIsUndoable() {
        CommandProcessor processor = fixture("before");
        processor.execute("checkpoint create initial");
        processor.execute("insert 0 6 \" after\"");
        assertEquals("before after", processor.getDocument().getParagraph(0).getText());
        assertEquals("Checkpoint restored: initial", processor.execute("checkpoint restore initial"));
        assertEquals("before", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        assertEquals("before after", processor.getDocument().getParagraph(0).getText());
        assertTrue(processor.execute("checkpoint restore missing").startsWith("ERROR:"));
    }

    @Test
    void replaceAllIsOneUndoableOperation() {
        CommandProcessor processor = fixture("cat scatter cat");
        processor.execute("replace-all substring \"cat\" \"dog\"");
        assertEquals("dog sdogter dog", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        assertEquals("cat scatter cat", processor.getDocument().getParagraph(0).getText());
        processor.execute("redo");
        assertEquals("dog sdogter dog", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void macroReplayIsOneUndoableOperation() {
        CommandProcessor processor = fixture("hello");
        processor.execute("macro start suffix");
        processor.execute("insert 0 5 \" world\"");
        processor.execute("format 0 0 5 bold true");
        processor.execute("macro stop");
        assertEquals("hello world", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        processor.execute("undo");
        assertEquals("hello", processor.getDocument().getParagraph(0).getText());
        int depthBeforeReplay = processor.getHistoryManager().getUndoDepth();
        processor.execute("macro replay suffix");
        assertEquals("hello world", processor.getDocument().getParagraph(0).getText());
        assertEquals(depthBeforeReplay + 1, processor.getHistoryManager().getUndoDepth());
        processor.execute("undo");
        assertEquals("hello", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void failedMacroReplayDoesNotPartiallyModifyTheDocument() {
        CommandProcessor processor = fixture("one");
        var document = processor.getDocument();
        var styles = processor.getStyleRegistry();
        var valid = command.EditCommand.insert(document, styles, 0, 3, " two");
        var invalid = command.EditCommand.delete(document, styles, 0, 999, 1000);
        var macro = new command.MacroCommand(document, styles, java.util.List.of(valid, invalid));
        assertThrows(IllegalArgumentException.class, macro::execute);
        assertEquals("one", document.getParagraph(0).getText());
    }

    @Test
    void macroCanIncludeNonMutatingRenderAndStatisticsActions() {
        CommandProcessor processor = fixture("hello world from macro");
        processor.execute("macro start inspect");
        processor.execute("render plain 80 greedy");
        processor.execute("stats");
        processor.execute("find whole_word \"macro\"");
        processor.execute("insert 0 22 \"!\"");
        processor.execute("macro stop");

        // Remove the independently recorded edit before replaying the saved macro.
        processor.execute("undo");
        String replay = processor.execute("macro replay inspect");
        assertTrue(replay.contains("hello world from macro"));
        assertTrue(replay.contains("Words:"));
        assertTrue(replay.contains("paragraph 0"));
        assertEquals("hello world from macro!", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        assertEquals("hello world from macro", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void macroPublishesOneChangeNotificationAfterTheWholeBatch() {
        CommandProcessor processor = fixture("base");
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        processor.getDocument().addChangeListener(ignored -> calls.incrementAndGet());
        processor.execute("macro start batch");
        processor.execute("insert 0 4 \" one\"");
        processor.execute("insert 0 8 \" two\"");
        processor.execute("macro stop");
        processor.execute("undo");
        processor.execute("undo");
        calls.set(0);
        assertTrue(processor.execute("macro replay batch").startsWith("Replayed macro:"));
        assertEquals(1, calls.get());
        assertEquals("base one two", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void emptyMacroRecordingIsRejectedWithoutSavingAnUnusableMacro() {
        CommandProcessor processor = fixture("text");
        assertTrue(processor.execute("macro start empty").startsWith("Recording"));
        assertTrue(processor.execute("macro stop").startsWith("ERROR:"));
        assertTrue(processor.getMacroManager().getMacroNames().isEmpty());
    }

    private static CommandProcessor fixture(String text) {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Main");
        processor.execute("paragraph add 0 " + command.EditCommand.quote(text));
        return processor;
    }
}