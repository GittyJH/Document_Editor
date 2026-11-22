package history;

import model.Document;
import model.DocumentSnapshot;
import model.FormatProperty;
import model.Formatting;
import model.FormattingSpan;
import model.Paragraph;
import model.Section;
import model.SectionElement;
import service.StyleRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Portable binary-to-Base64 codec for a complete document and inherited-style registry state. */
public final class EditorStateCodec {
    private static final int VERSION = 1;
    private EditorStateCodec() { }

    /** Encodes the supplied document and style registry into a self-contained state token. */
    public static String encode(DocumentSnapshot document, StyleRegistry.State styles) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(VERSION);
            writeDocument(output, document.toDocument());
            Map<String, Formatting> styleMap = styles == null ? Map.of() : styles.getStyles();
            output.writeInt(styleMap.size());
            for (Map.Entry<String, Formatting> entry : styleMap.entrySet()) {
                writeString(output, entry.getKey());
                writeFormatting(output, entry.getValue());
            }
            output.flush();
            return "@STATE|" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode document state.", exception);
        }
    }

    /** Decodes a state token and restores its document and style values. */
    public static DecodedState decode(String token) {
        if (token == null || !token.startsWith("@STATE|")) throw new IllegalArgumentException("Invalid document-state token.");
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token.substring(7));
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
            if (input.readInt() != VERSION) throw new IllegalArgumentException("Unsupported document-state version.");
            Document document = readDocument(input);
            StyleRegistry registry = new StyleRegistry();
            int styleCount = readCount(input);
            for (int i = 0; i < styleCount; i++) registry.define(readString(input), readFormatting(input));
            if (input.available() != 0) throw new IllegalArgumentException("Unexpected trailing data in document state.");
            return new DecodedState(document.snapshot(), registry.snapshot());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid serialized document state: " + exception.getMessage(), exception);
        }
    }

    /** Holds a decoded document snapshot and style-registry snapshot. */
    public static final class DecodedState {
        private final DocumentSnapshot document;
        private final StyleRegistry.State styles;
        private DecodedState(DocumentSnapshot document, StyleRegistry.State styles) {
            this.document = document;
            this.styles = styles;
        }
        /** Returns the decoded document snapshot. */ public DocumentSnapshot getDocument() { return document; }
        /** Returns the decoded style registry state. */ public StyleRegistry.State getStyles() { return styles; }
    }

    private static void writeDocument(DataOutputStream out, Document document) throws IOException {
        writeString(out, document.getTitle());
        out.writeInt(document.getSections().size());
        for (Section section : document.getSections()) writeSection(out, section);
    }

    private static Document readDocument(DataInputStream in) throws IOException {
        Document document = new Document(readString(in));
        int count = readCount(in);
        for (int i = 0; i < count; i++) document.addSection(readSection(in));
        return document;
    }

    private static void writeSection(DataOutputStream out, Section section) throws IOException {
        writeString(out, section.getTitle());
        writeFormatting(out, section.getFormatting());
        writeString(out, section.getStyleName());
        out.writeInt(section.getElements().size());
        for (SectionElement child : section.getElements()) {
            if (child instanceof Section nested) {
                out.writeByte(1);
                writeSection(out, nested);
            } else if (child instanceof Paragraph paragraph) {
                out.writeByte(2);
                writeString(out, paragraph.getText());
                writeFormatting(out, paragraph.getFormatting());
                var spans = paragraph.getFormattedContent().getFormattingSpans();
                out.writeInt(spans.size());
                for (FormattingSpan span : spans) {
                    out.writeInt(span.getStart());
                    out.writeInt(span.getEnd());
                    writeFormatting(out, span.getFormatting());
                }
            }
        }
    }

    private static Section readSection(DataInputStream in) throws IOException {
        Section section = new Section(readString(in));
        section.setFormatting(readFormatting(in));
        section.setStyleName(readString(in));
        int count = readCount(in);
        for (int i = 0; i < count; i++) {
            int kind = in.readUnsignedByte();
            if (kind == 1) section.addElement(readSection(in));
            else if (kind == 2) {
                Paragraph paragraph = section.addParagraph(readString(in));
                paragraph.setFormatting(readFormatting(in));
                int spanCount = readCount(in);
                for (int j = 0; j < spanCount; j++) {
                    int start = in.readInt();
                    int end = in.readInt();
                    paragraph.applyFormatting(start, end, readFormatting(in));
                }
            } else throw new IllegalArgumentException("Unknown section child kind: " + kind);
        }
        return section;
    }

    private static void writeFormatting(DataOutputStream out, Formatting value) throws IOException {
        writeNullableBoolean(out, value.getBold());
        writeNullableBoolean(out, value.getItalic());
        writeString(out, value.getFontName());
        out.writeInt(value.getFontSize() == null ? -1 : value.getFontSize());
        writeString(out, value.getColor());
    }

    private static Formatting readFormatting(DataInputStream in) throws IOException {
        Boolean bold = readNullableBoolean(in);
        Boolean italic = readNullableBoolean(in);
        String font = readString(in);
        int size = in.readInt();
        String color = readString(in);
        return new Formatting(bold, italic, font, size < 0 ? null : size, color);
    }

    private static void writeNullableBoolean(DataOutputStream out, Boolean value) throws IOException {
        out.writeByte(value == null ? 0 : value ? 2 : 1);
    }

    private static Boolean readNullableBoolean(DataInputStream in) throws IOException {
        return switch (in.readUnsignedByte()) {
            case 0 -> null;
            case 1 -> false;
            case 2 -> true;
            default -> throw new IllegalArgumentException("Invalid Boolean value.");
        };
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) { out.writeInt(-1); return; }
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) return null;
        if (length < 0 || length > 10_000_000 || length > in.available()) throw new IllegalArgumentException("Invalid string length.");
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static int readCount(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 1_000_000) throw new IllegalArgumentException("Invalid collection size.");
        return count;
    }
}