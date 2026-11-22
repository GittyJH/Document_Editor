import actor.Author;
import actor.Reviewer;
import command.CommandProcessor;
import command.TextCommandInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the Author and Reviewer actor adapters over the shared command interface. */
class ActorAndInterfaceTest {
    @Test
    void authorCreatesAndEditsWhileReviewerViewsAndRestoresCheckpoint() {
        CommandProcessor processor = new CommandProcessor();
        TextCommandInterface commandInterface = new TextCommandInterface(processor);
        Author author = new Author("author-1", "Alice", commandInterface);
        Reviewer reviewer = new Reviewer("reviewer-1", "Bob", commandInterface);

        assertTrue(author.createDocument("Review Draft").startsWith("OK:"));
        assertTrue(author.editDocument("section add root Introduction").startsWith("OK:"));
        assertTrue(author.editDocument("paragraph add 0 \"first version\"").startsWith("OK:"));
        author.manageHistory("checkpoint create \"review point\"");
        author.editDocument("insert 0 5 \" revised\"");

        assertTrue(reviewer.viewDocument(80).contains("first revised version"));
        assertEquals("Checkpoint restored: review point", reviewer.restoreCheckpoint("review point"));
        assertTrue(reviewer.viewDocument(80).contains("first version"));
        assertEquals("reviewer-1", reviewer.getId());
        assertEquals("Bob", reviewer.getName());
        assertEquals("Alice", author.getName());
    }

    @Test
    void actorRejectsMissingIdentityOrCommandInterface() {
        CommandProcessor processor = new CommandProcessor();
        assertThrows(IllegalArgumentException.class, () -> new Author("", "Alice", new TextCommandInterface(processor)));
        assertThrows(IllegalArgumentException.class, () -> new Reviewer("r", "", new TextCommandInterface(processor)));
        assertThrows(IllegalArgumentException.class, () -> new Author("a", "Alice", null));
    }
}