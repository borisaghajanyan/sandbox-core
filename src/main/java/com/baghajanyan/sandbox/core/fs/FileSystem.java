package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface FileSystem {
    Path createTempFile(String prefix, String suffix) throws IOException;

    boolean notExists(Path path);

    void delete(Path path) throws IOException;
}
