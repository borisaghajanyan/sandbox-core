package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface FileSystem {
    /**
     * Creates a temporary file with the given prefix and suffix.
     * 
     * @param prefix The prefix string to be used in generating the file's name, may
     *               be {@code null}.
     * @param suffix The suffix string to be used in generating the file's name, may
     *               be {@code null}, in which case"{@code .tmp}" is used.
     * @return The {@link Path} to the newly created temporary file.
     * @throws IOException If an I/O error occurs.
     */
    Path createTempFile(String prefix, String suffix) throws IOException;

    /**
     * Writes the given content to the specified path.
     * 
     * @param path    The {@link Path} to write the content to.
     * @param content The content to be written.
     * @throws IOException If an I/O error occurs while writing.
     */
    void write(Path path, String content) throws IOException;

    /**
     * Checks if the specified path does not exist.
     * 
     * @param path The {@link Path} to check.
     * @return true if the path does not exist, false otherwise.
     */
    boolean notExists(Path path);

    /**
     * Deletes the file or directory at the specified path.
     * 
     * @param path The {@link Path} to the file or directory to be deleted.
     * @throws IOException If an I/O error occurs.
     */
    void delete(Path path) throws IOException;
}
