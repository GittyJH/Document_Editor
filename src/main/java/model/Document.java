package model;

import service.StatisticsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The root of a structured document containing recursively nested sections. */
public final class Document {
    private String title;
    private final List<Section> sections = new ArrayList<>();
    private final List<DocumentChangeListener> changeListeners = new ArrayList<>();
    private int notificationBatchDepth;

    /** Creates an empty document with the supplied title. */
    public Document(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Document title cannot be empty.");
        this.title = title.trim();
    }

    /** Returns the document title. */ public String getTitle() { return title; }
    /** Changes the document title. */
    public void setTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Document title cannot be empty.");
        this.title = title.trim();
    }

    /** Creates and appends a new root section. */
    public Section addSection(String title) {
        Section section = new Section(title);
        sections.add(section);
        return section;
    }

    /** Adds a detached section as a root section. */
    public void addSection(Section section) {
        Objects.requireNonNull(section, "section");
        if (section.getParent() != null || sections.contains(section)) throw new IllegalArgumentException("Section is already attached.");
        sections.add(section);
    }

    /** Removes a root section, rejecting a section that is not directly in this document. */
    public void removeSection(Section section) {
        if (!sections.remove(section)) throw new IllegalArgumentException("Section is not a root section of this document.");
    }

    /** Returns a root section by zero-based index. */
    public Section getSection(int index) { return sections.get(index); }
    /** Returns an immutable view of the root sections. */ public List<Section> getSections() { return Collections.unmodifiableList(sections); }

    /** Resolves a zero-based dotted path such as {@code 0.2.1}. */
    public Section getSectionByPath(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Section path cannot be empty.");
        String[] parts = path.split("\\.", -1);
        int index;
        try { index = Integer.parseInt(parts[0]); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid section path: " + path); }
        if (index < 0 || index >= sections.size()) throw new IllegalArgumentException("Section path does not exist: " + path);
        Section current = sections.get(index);
        for (int i = 1; i < parts.length; i++) {
            try { index = Integer.parseInt(parts[i]); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid section path: " + path); }
            List<SectionElement> children = current.getElements();
            List<Section> nested = new ArrayList<>();
            for (SectionElement element : children) if (element instanceof Section section) nested.add(section);
            if (index < 0 || index >= nested.size()) throw new IllegalArgumentException("Section path does not exist: " + path);
            current = nested.get(index);
        }
        return current;
    }

    /** Returns a paragraph by depth-first, zero-based paragraph index. */
    public Paragraph getParagraph(int index) {
        List<Paragraph> paragraphs = getParagraphs();
        if (index < 0 || index >= paragraphs.size()) throw new IllegalArgumentException("Paragraph index does not exist: " + index);
        return paragraphs.get(index);
    }

    /** Returns all paragraphs in depth-first document order. */
    public List<Paragraph> getParagraphs() {
        List<Paragraph> result = new ArrayList<>();
        for (Section section : sections) result.addAll(section.getParagraphs());
        return Collections.unmodifiableList(result);
    }

    /** Returns a paragraph's zero-based depth-first index, or {@code -1} if it is not in this document. */
    public int indexOf(Paragraph paragraph) {
        List<Paragraph> paragraphs = getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) if (paragraphs.get(i) == paragraph) return i;
        return -1;
    }

    /** Searches all paragraphs using the requested matching mode. */
    public List<Match> findMatches(String query, MatchMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (query == null || query.isEmpty()) throw new IllegalArgumentException("Search text cannot be empty.");
        List<Match> result = new ArrayList<>();
        Pattern pattern = mode == MatchMode.SUBSTRING
                ? Pattern.compile(Pattern.quote(query))
                : Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(query) + "(?![A-Za-z0-9_])");
        for (Paragraph paragraph : getParagraphs()) {
            Matcher matcher = pattern.matcher(paragraph.getText());
            while (matcher.find()) result.add(new Match(paragraph, matcher.start(), matcher.end()));
        }
        return Collections.unmodifiableList(result);
    }

    /** Calculates current statistics through the independent statistics service. */
    public DocumentStatistics getStatistics() { return new StatisticsService().calculate(this); }

    /** Captures a deep snapshot of the current document. */ public DocumentSnapshot snapshot() { return new DocumentSnapshot(this); }

    /** Replaces all document state with the state captured by a snapshot. */
    public void restoreFrom(DocumentSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        title = snapshot.getTitle();
        sections.clear();
        for (Section section : snapshot.getSections()) sections.add(section);
    }

    /** Returns a deep copy of this document. */ public Document deepCopy() { return snapshot().toDocument(); }

    /** Registers a document-change observer. Duplicate registrations are ignored. */
    public void addChangeListener(DocumentChangeListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!changeListeners.contains(listener)) changeListeners.add(listener);
    }

    /** Removes a previously registered document-change observer. */
    public void removeChangeListener(DocumentChangeListener listener) { changeListeners.remove(listener); }

    /** Begins a nested batch that suppresses intermediate document-change notifications. */
    public void beginChangeNotificationBatch() { notificationBatchDepth++; }

    /** Ends a previously opened notification batch. The enclosing command publishes once on commit. */
    public void endChangeNotificationBatch() {
        if (notificationBatchDepth <= 0) throw new IllegalStateException("No document-change notification batch is active.");
        notificationBatchDepth--;
    }

    /** Notifies observers after a complete high-level document command changes state. */
    public void publishChange() {
        if (notificationBatchDepth > 0) return;
        for (DocumentChangeListener listener : List.copyOf(changeListeners)) {
            try { listener.documentChanged(this); }
            catch (RuntimeException ignored) { /* Observers must not invalidate committed document operations. */ }
        }
    }

    /** Returns the total count of distinct formatting spans in paragraph text and section titles. */
    public int getStoredFormattingSpanCount() {
        int count = 0;
        for (Section section : sections) count += countSpans(section);
        return count;
    }

    private static int countSpans(Section section) {
        int count = section.getFormatting().isEmpty() ? 0 : 1;
        for (SectionElement child : section.getElements()) {
            if (child instanceof Paragraph paragraph) count += paragraph.getFormattedContent().getFormattingSpans().size();
            else if (child instanceof Section nested) count += countSpans(nested);
        }
        return count;
    }
}