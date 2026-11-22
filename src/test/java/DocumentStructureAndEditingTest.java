import command.CommandProcessor;
import model.Document;
import model.Paragraph;
import model.Section;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** JUnit coverage for the composite document hierarchy and editing commands. */
class DocumentStructureAndEditingTest {
    @Test
    void supportsArbitrarilyNestedSectionsAndRecursiveTraversal() {
        Document document = new Document("Report");
        Section root = document.addSection("Introduction");
        root.addParagraph("Root paragraph");
        Section levelOne = root.addSection("Level one");
        Section levelTwo = levelOne.addSection("Level two");
        Section levelThree = levelTwo.addSection("Level three");
        levelThree.addParagraph("Deep content");

        assertEquals("Level three", document.getSectionByPath("0.0.0.0").getTitle());
        assertEquals(2, document.getParagraphs().size());
        assertEquals("Deep content", document.getParagraph(1).getText());
        assertEquals(2, document.getStatistics().getParaCnt());
        assertEquals(11, document.getStatistics().getWordCnt());
    }

    @Test
    void commandInterfaceCreatesAndEditsDocumentText() {
        CommandProcessor processor = new CommandProcessor();
        assertTrue(processor.execute("document create \"Example\"").startsWith("OK:"));
        assertTrue(processor.execute("section add root \"Main\"").startsWith("OK:"));
        assertTrue(processor.execute("paragraph add 0 \"abcdef\"").startsWith("OK:"));

        processor.execute("insert 0 3 \"XYZ\"");
        assertEquals("abcXYZdef", processor.getDocument().getParagraph(0).getText());
        processor.execute("delete 0 1 3");
        assertEquals("aXYZdef", processor.getDocument().getParagraph(0).getText());
        processor.execute("edit-replace 0 1 4 \"123\"");
        assertEquals("a123def", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void moveUsesPreMoveDestinationCoordinates() {
        CommandProcessor processor = fixture("abcdefgh");
        assertEquals("OK: MOVE paragraph 0", processor.execute("move 0 1 3 7"));
        assertEquals("adefgbch", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void splitAndMergeOperateOnImmediateSiblingParagraphs() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root S");
        processor.execute("paragraph add 0 \"abcdef\"");
        processor.execute("split 0 2");
        assertEquals(2, processor.getDocument().getParagraphs().size());
        assertEquals("ab", processor.getDocument().getParagraph(0).getText());
        assertEquals("cdef", processor.getDocument().getParagraph(1).getText());
        processor.execute("merge 0");
        assertEquals(1, processor.getDocument().getParagraphs().size());
        assertEquals("abcdef", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void failedEditsAreAtomicAndMergeAtSectionEndIsRejected() {
        CommandProcessor processor = fixture("safe");
        String before = processor.getDocument().getParagraph(0).getText();
        int depthBefore = processor.getHistoryManager().getUndoDepth();
        assertTrue(processor.execute("insert 0 99 \"bad\"").startsWith("ERROR:"));
        assertTrue(processor.execute("delete 0 2 99").startsWith("ERROR:"));
        assertTrue(processor.execute("merge 0").startsWith("ERROR:"));
        assertEquals(before, processor.getDocument().getParagraph(0).getText());
        assertEquals(depthBefore, processor.getHistoryManager().getUndoDepth());
    }

    @Test
    void emptyParagraphSupportsBoundaryInsertionsAndEmptyDeletion() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root S");
        processor.execute("paragraph add 0 \"\"");
        assertTrue(processor.execute("insert 0 0 \"x\"").startsWith("OK:"));
        assertEquals("x", processor.getDocument().getParagraph(0).getText());
        assertTrue(processor.execute("delete 0 0 1").startsWith("OK:"));
        assertEquals("", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void compositeRejectsSelfAndAncestorCyclesAndMalformedPaths() {
        Document document = new Document("Cycles");
        Section root = document.addSection("Root");
        Section child = root.addSection("Child");
        assertThrows(IllegalArgumentException.class, () -> root.addElement(root));
        assertThrows(IllegalArgumentException.class, () -> child.addElement(root));
        assertThrows(IllegalArgumentException.class, () -> document.getSectionByPath("0."));
        assertEquals("Child", document.getSectionByPath("0.0").getTitle());
        assertEquals(1, document.getSections().size());
    }

    @Test
    void sectionRemovalCanBeUndoneAndRedoneWithItsEntireSubtree() {
        CommandProcessor processor = fixture("nested text");
        processor.execute("section add 0 Child");
        processor.execute("paragraph add 0.0 \"child paragraph\"");
        int paragraphCount = processor.getDocument().getStatistics().getParaCnt();
        processor.execute("section remove 0");
        assertTrue(processor.getDocument().getSections().isEmpty());
        processor.execute("undo");
        assertEquals(2, processor.getDocument().getStatistics().getParaCnt());
        assertEquals("child paragraph", processor.getDocument().getParagraph(1).getText());
        processor.execute("redo");
        assertTrue(processor.getDocument().getSections().isEmpty());
        assertEquals(0, processor.getDocument().getStatistics().getParaCnt());
        assertEquals(2, paragraphCount);
    }

    @Test
    void invalidMoveDestinationIsRejectedWithoutChangingText() {
        CommandProcessor processor = fixture("abcdefgh");
        assertTrue(processor.execute("move 0 1 5 3").startsWith("ERROR:"));
        assertTrue(processor.execute("move 0 1 5 99").startsWith("ERROR:"));
        assertEquals("abcdefgh", processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void documentChangeListenersObserveCommittedCommandsAndUndoRedo() {
        CommandProcessor processor = fixture("abc");
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        model.DocumentChangeListener listener = ignored -> calls.incrementAndGet();
        processor.getDocument().addChangeListener(listener);
        processor.execute("insert 0 3 \"d\"");
        processor.execute("undo");
        processor.execute("redo");
        assertEquals(3, calls.get());
        processor.getDocument().removeChangeListener(listener);
        processor.execute("undo");
        assertEquals(3, calls.get());
    }

    @Test
    void failingChangeListenerCannotBreakCommittedEdit() {
        CommandProcessor processor = fixture("abc");
        processor.getDocument().addChangeListener(ignored -> { throw new IllegalStateException("observer failure"); });
        assertTrue(processor.execute("insert 0 3 \"d\"").startsWith("OK:"));
        assertEquals("abcd", processor.getDocument().getParagraph(0).getText());
        assertTrue(processor.execute("undo").startsWith("Undone"));
        assertEquals("abc", processor.getDocument().getParagraph(0).getText());
    }

    private static CommandProcessor fixture(String text) {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root S");
        processor.execute("paragraph add 0 " + command.EditCommand.quote(text));
        return processor;
    }
}