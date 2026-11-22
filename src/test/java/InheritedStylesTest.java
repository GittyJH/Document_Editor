import command.CommandProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests UC-10 section-level style inheritance, paragraph overrides, and live style updates. */
class InheritedStylesTest {
    @Test
    void paragraphInheritsStyleFromContainingAndNestedSections() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Root");
        processor.execute("style define body font \"Courier New\"");
        processor.execute("style apply 0 body");
        processor.execute("section add 0 Nested");
        processor.execute("paragraph add 0.0 \"nested content\"");
        String markup = processor.execute("render markup 80 greedy");
        assertTrue(markup.contains("font=Courier New"));
    }

    @Test
    void paragraphOverridesTakePrecedenceAndCanFallBackToInheritedValues() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Root");
        processor.execute("paragraph add 0 \"colored words\"");
        processor.execute("style define body color blue");
        processor.execute("style apply 0 body");
        processor.execute("format 0 0 7 color red");
        String markup = processor.execute("render markup 80 greedy");
        assertTrue(markup.contains("color=red"));
        assertTrue(markup.contains("color=blue"));
        processor.execute("format 0 0 7 color inherit");
        markup = processor.execute("render markup 80 greedy");
        assertFalse(markup.contains("color=red"));
        assertTrue(markup.contains("color=blue"));
    }

    @Test
    void changingNamedStyleUpdatesAllInheritingParagraphs() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Root");
        processor.execute("paragraph add 0 \"first\"");
        processor.execute("section add 0 Nested");
        processor.execute("paragraph add 0.0 \"second\"");
        processor.execute("style define body font Serif");
        processor.execute("style apply 0 body");
        processor.execute("style define body font Monospace");
        String markup = processor.execute("render markup 80 greedy");
        assertEquals(4, occurrences(markup, "font=Monospace"));
        assertFalse(markup.contains("font=Serif"));
    }

    @Test
    void styleDefinitionAndApplicationAreUndoable() {
        CommandProcessor processor = new CommandProcessor();
        processor.execute("section add root Root");
        processor.execute("style define body color green");
        processor.execute("style apply 0 body");
        assertTrue(processor.getStyleRegistry().getStyleNames().contains("body"));
        processor.execute("undo");
        assertNull(processor.getDocument().getSection(0).getStyleName());
        processor.execute("undo");
        assertFalse(processor.getStyleRegistry().getStyleNames().contains("body"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) { count++; index += needle.length(); }
        return count;
    }
}