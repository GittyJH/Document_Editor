import command.CommandProcessor;
import model.FormatProperty;
import model.Formatting;
import model.Paragraph;
import render.GreedyLineBreak;
import render.HyphenationLineBreak;
import render.RenderMode;
import service.StyleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests range formatting, section-title formatting, render views, and line-breaking strategies. */
class FormattingAndRenderingTest {
    @Test
    void formattingCrossesExistingBoundariesWithoutChangingCharacters() {
        CommandProcessor processor = fixture("abcdefghij");
        processor.execute("format 0 0 3 bold true");
        processor.execute("format 0 7 10 italic true");
        String text = processor.getDocument().getParagraph(0).getText();
        processor.execute("format 0 2 9 color red");

        Paragraph paragraph = processor.getDocument().getParagraph(0);
        assertEquals(text, paragraph.getText());
        assertEquals("red", paragraph.getFormattedContent().getFormattingSpans().get(1).getFormatting().getColor());
        String markup = processor.execute("render markup 80 greedy");
        assertTrue(markup.contains("**"));
        assertTrue(markup.contains("{font=Arial,size=12,color=red}"));
    }

    @Test
    void formattingCanBeRemovedAndSectionTitlesCanBeFormatted() {
        CommandProcessor processor = fixture("hello world");
        processor.execute("format 0 0 5 bold true");
        assertTrue(processor.execute("format 0 0 5 bold inherit").startsWith("OK:"));
        processor.execute("format-title 0 size 18");
        String markup = processor.execute("render markup 80 greedy");
        assertTrue(markup.contains("size=18"));
        assertFalse(markup.contains("**hello**"));
    }

    @Test
    void greedyAndHyphenationStrategiesCanProduceDifferentOutput() {
        String text = "alpha supercalifragilistic beta";
        String greedy = String.join("|", new GreedyLineBreak().breakLine(text, 8));
        String hyphen = String.join("|", new HyphenationLineBreak().breakLine(text, 8));
        assertNotEquals(greedy, hyphen);
        assertTrue(hyphen.contains("-"));
        assertTrue(greedy.contains("supercalifragilistic"));
    }

    @Test
    void plainRenderingOmitsMarkersAndDoesNotMutateDocument() {
        CommandProcessor processor = fixture("alpha beta gamma");
        processor.execute("format 0 0 5 bold true");
        String before = processor.getDocument().getParagraph(0).getText();
        String plain = processor.execute("render plain 8 greedy");
        String marked = processor.execute("render markup 80 greedy");
        assertFalse(plain.contains("**"));
        assertTrue(marked.contains("**alpha**"));
        assertEquals(before, processor.getDocument().getParagraph(0).getText());
    }

    @Test
    void invalidWidthsAreRejectedBeforeRendering() {
        CommandProcessor processor = fixture("small text");
        assertTrue(processor.execute("render plain 0 greedy").startsWith("ERROR:"));
        assertTrue(processor.execute("render plain -3 greedy").startsWith("ERROR:"));
    }

    @Test
    void styleFormattingOverlaySupportsExplicitFalseValues() {
        Formatting inherited = new Formatting(true, false, "Serif", 14, "blue");
        Formatting override = new Formatting(false, null, null, null, null);
        Formatting resolved = override.resolveAgainst(inherited);
        assertEquals(Boolean.FALSE, resolved.getBold());
        assertEquals(Boolean.FALSE, resolved.getItalic());
        assertEquals("Serif", resolved.getFontName());
    }

    @Test
    void invalidFormattingRangeDoesNotChangeTextFormattingOrHistory() {
        CommandProcessor processor = fixture("unchanged");
        int depth = processor.getHistoryManager().getUndoDepth();
        assertTrue(processor.execute("format 0 0 100 bold true").startsWith("ERROR:"));
        assertTrue(processor.getDocument().getParagraph(0).getFormattedContent().getFormattingSpans().isEmpty());
        assertEquals("unchanged", processor.getDocument().getParagraph(0).getText());
        assertEquals(depth, processor.getHistoryManager().getUndoDepth());
    }

    @Test
    void rendererAcceptsAdditionalViewsAndKeepsNarrowNestedOutputWithinWidth() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root R");
        processor.execute("paragraph add 0 x");
        processor.execute("section add 0 N");
        processor.execute("paragraph add 0.0 x");
        String narrow = processor.getRenderer().render(processor.getDocument(), RenderMode.PLAIN_TEXT,
                2, new GreedyLineBreak());
        for (String line : narrow.split("\\R")) assertTrue(line.length() <= 2);

        processor.getRenderer().registerView(RenderMode.PLAIN_TEXT,
                (document, width, strategy) -> "custom-view:" + document.getTitle() + ":" + width);
        assertEquals("custom-view:Untitled:23", processor.getRenderer().render(processor.getDocument(),
                RenderMode.PLAIN_TEXT, 23, new GreedyLineBreak()));
    }

    @Test
    void strategiesRejectNullTextAndInvalidWidths() {
        assertThrows(IllegalArgumentException.class, () -> new GreedyLineBreak().breakLine("x", 0));
        assertThrows(IllegalArgumentException.class, () -> new HyphenationLineBreak().breakLine(null, 5));
        assertThrows(IllegalArgumentException.class, () -> new Formatting().set(FormatProperty.SIZE, "0"));
    }


    @Test
    void formattedTextAccessorsRepresentElementAndRangeFormatting() {
        CommandProcessor processor = fixture("hello world");
        processor.getDocument().getSection(0).setFormatting(new Formatting(true, null, null, null, null));
        processor.getDocument().getParagraph(0).setFormatting(new Formatting(null, true, null, null, null));
        processor.execute("format 0 0 5 color red");
        assertTrue(processor.getDocument().getSection(0).getFormattedText().startsWith("**Main**"));
        String paragraph = processor.getDocument().getParagraph(0).getFormattedText();
        assertTrue(paragraph.contains("*{font=Arial,size=12,color=red}hello"));
        assertTrue(paragraph.contains("color=red"));
    }

    private static CommandProcessor fixture(String text) {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Main");
        processor.execute("paragraph add 0 " + command.EditCommand.quote(text));
        return processor;
    }
}