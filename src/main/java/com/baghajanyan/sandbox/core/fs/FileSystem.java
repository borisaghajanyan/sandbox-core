package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface FileSystem {
    /**
     * Creates a temporary file with the given prefix and suffix.
     * 
     * @param prefix The prefix string to be used in generating the file's name.
     * @param suffix The suffix string to be used in generating the file's name.
     * @return The path to the newly created temporary file.
     * @throws IOException If an I/O error occurs.
     */
    Path createTempFile(String prefix, String suffix) throws IOException;

    /**
     * Checks if the specified path does not exist.
     * 
     * @param path The path to check.
     * @return true if the path does not exist, false otherwise.
     */
    boolean notExists(Path path);

    /**
     * Deletes the file or directory at the specified path.
     * 
     * @param path The path to the file or directory to be deleted.
     * @throws IOException If an I/O error occurs.
     */
    void delete(Path path) throws IOException;
}
