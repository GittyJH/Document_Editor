import command.CommandProcessor;
import command.ScriptRunner;
import command.TextCommandInterface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests deterministic script execution and the explicit 100,000-character capacity target. */
class ScriptAndCapacityTest {
    @Test
    void identicalScriptsProduceIdenticalOutputAndDocumentState() {
        List<String> script = List.of(
                "document create \"Script Test\"",
                "section add root Intro",
                "paragraph add 0 \"repeatable output from scripts\"",
                "format 0 0 10 bold true",
                "render markup 20 greedy",
                "stats");
        CommandProcessor first = new CommandProcessor();
        CommandProcessor second = new CommandProcessor();
        List<String> one = new ScriptRunner().runLines(script, new TextCommandInterface(first));
        List<String> two = new ScriptRunner().runLines(script, new TextCommandInterface(second));
        assertEquals(one, two);
        assertEquals(first.getDocument().getParagraph(0).getText(), second.getDocument().getParagraph(0).getText());
    }

    @Test
    void scriptStopsAtInvalidCommandAndReportsLineNumber() {
        CommandProcessor processor = new CommandProcessor();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ScriptRunner().runLines(List.of("section add root A", "paragraph add 99 bad", "stats"),
                        new TextCommandInterface(processor)));
        assertTrue(error.getMessage().contains("line 2"));
        assertEquals(0, processor.getDocument().getParagraphs().size());
    }

    @Test
    void commandParsingIsStableUnderTurkishDefaultLocale() {
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            CommandProcessor processor = new CommandProcessor();
            assertTrue(processor.execute("SECTION ADD ROOT Intro").startsWith("OK:"));
            assertTrue(processor.execute("PARAGRAPH ADD 0 \"International\"").startsWith("OK:"));
            assertTrue(processor.execute("FORMAT 0 0 5 BOLD TRUE").startsWith("OK:"));
            assertTrue(processor.execute("RENDER MARKUP 80 GREEDY").contains("**Inter**"));
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void loadsRendersAndSearchesAtLeastOneHundredThousandCharacters() {
        String text = "word ".repeat(20_000);
        CommandProcessor processor = new CommandProcessor();
        assertTrue(processor.execute("section add root Capacity").startsWith("OK:"));
        assertTrue(processor.execute("paragraph add 0 " + command.EditCommand.quote(text)).startsWith("OK:"));
        assertEquals(100_008, processor.getDocument().getStatistics().getCharCnt());
        assertEquals(20_001, processor.getDocument().getStatistics().getWordCnt());
        assertEquals(20_000, processor.getDocument().findMatches("word", model.MatchMode.WHOLE_WORD).size());
        String rendered = processor.execute("render plain 80 greedy");
        assertFalse(rendered.startsWith("ERROR:"));
        assertTrue(rendered.length() > 100_000);
    }
}