package history;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local UTF-8 file-system implementation. */
public final class LocalFileSystem implements FileSystem {
    /** Reads a file as UTF-8 text. */
    @Override public String read(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("File cannot be null.");
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
    /** Writes a file as UTF-8 text, creating parent directories when needed. */
    @Override public void write(File file, String data) throws IOException {
        if (file == null || data == null) throw new IllegalArgumentException("File and data are required.");
        Path path = file.toPath().toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(path, data, StandardCharsets.UTF_8);
    }
}