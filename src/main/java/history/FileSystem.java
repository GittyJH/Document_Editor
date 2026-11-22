package history;

import java.io.File;
import java.io.IOException;

/** File-system abstraction used by history and macro persistence. */
public interface FileSystem {
    /** Reads the complete UTF-8 text content of a file. */
    String read(File file) throws IOException;
    /** Writes UTF-8 text content, replacing any previous file content. */
    void write(File file, String data) throws IOException;
}