package service;

import model.Document;
import model.DocumentStatistics;
import model.Paragraph;
import model.Section;
import model.SectionElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Independently traverses document content to calculate current statistics. */
public final class StatisticsService {
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9]+(?:'[A-Za-z0-9]+)?");

    /** Calculates word, character, and paragraph counts from the supplied document. */
    public DocumentStatistics calculate(Document document) {
        if (document == null) throw new IllegalArgumentException("Document cannot be null.");
        int[] counts = new int[3];
        for (Section section : document.getSections()) visit(section, counts);
        return new DocumentStatistics(counts[0], counts[1], counts[2]);
    }

    private void visit(Section section, int[] counts) {
        addText(section.getTitle(), counts);
        for (SectionElement element : section.getElements()) {
            if (element instanceof Paragraph paragraph) {
                counts[1] += paragraph.getText().length();
                counts[2]++;
                addWords(paragraph.getText(), counts);
            } else if (element instanceof Section nested) {
                visit(nested, counts);
            }
        }
    }

    private void addText(String text, int[] counts) {
        counts[1] += text.length();
        addWords(text, counts);
    }

    private void addWords(String text, int[] counts) {
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) counts[0]++;
    }
}