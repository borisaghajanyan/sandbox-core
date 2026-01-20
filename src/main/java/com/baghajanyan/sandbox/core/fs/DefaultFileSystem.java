package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Default implementation of the {@link FileSystem} interface using Java NIO.
 */
public class DefaultFileSystem implements FileSystem {
    public Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    public boolean notExists(Path path) {
        return Files.notExists(path);
    }

    public void write(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }
}
