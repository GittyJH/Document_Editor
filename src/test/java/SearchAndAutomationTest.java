import command.CommandProcessor;
import model.MatchMode;
import service.SearchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests substring/whole-word search and nested Replace First/Replace All behavior. */
class SearchAndAutomationTest {
    @Test
    void substringAndWholeWordSearchUseDifferentBoundaries() {
        CommandProcessor processor = fixture("cat scatter catapult cat");
        SearchService search = new SearchService();
        assertEquals(4, search.find(processor.getDocument(), "cat", MatchMode.SUBSTRING).size());
        assertEquals(2, search.find(processor.getDocument(), "cat", MatchMode.WHOLE_WORD).size());
    }

    @Test
    void replaceFirstAndReplaceAllWorkAcrossNestedContent() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Root");
        processor.execute("paragraph add 0 \"alpha dog\"");
        processor.execute("section add 0 Nested");
        processor.execute("paragraph add 0.0 \"dog beta dog\"");
        processor.execute("replace-first whole_word dog fox");
        assertEquals("alpha fox", processor.getDocument().getParagraph(0).getText());
        assertEquals("dog beta dog", processor.getDocument().getParagraph(1).getText());
        processor.execute("replace-all whole_word dog wolf");
        assertEquals("wolf beta wolf", processor.getDocument().getParagraph(1).getText());
        processor.execute("undo");
        assertEquals("dog beta dog", processor.getDocument().getParagraph(1).getText());
    }

    @Test
    void noMatchAndEmptyQueryFailWithoutHistoryChanges() {
        CommandProcessor processor = fixture("ordinary text");
        int depth = processor.getHistoryManager().getUndoDepth();
        assertTrue(processor.execute("replace-all substring missing found").startsWith("ERROR:"));
        assertTrue(processor.execute("find substring \"\"").startsWith("ERROR:"));
        assertEquals(depth, processor.getHistoryManager().getUndoDepth());
        assertEquals("ordinary text", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void emptyReplacementIsValidAndUndoable() {
        CommandProcessor processor = fixture("remove me");
        assertTrue(processor.execute("replace-first substring \"remove \" \"\"").startsWith("OK:"));
        assertEquals("me", processor.getDocument().getParagraph(0).getText());
        processor.execute("undo");
        assertEquals("remove me", processor.getDocument().getParagraph(0).getText());
    }

    private static CommandProcessor fixture(String text) {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Main");
        processor.execute("paragraph add 0 " + command.EditCommand.quote(text));
        return processor;
    }
}