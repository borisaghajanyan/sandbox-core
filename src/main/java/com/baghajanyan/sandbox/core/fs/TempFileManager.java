package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TempFileManager {
    /**
     * Executor service for handling asynchronous file deletions.
     */
    private static final ExecutorService DELETE_EXECUTOR = Executors.newSingleThreadExecutor(
            task -> {
                var thread = new Thread(task, "temp-file-delete-thread");
                thread.setDaemon(true);
                return thread;
            });

    private FileSystem fileSystem;
    private int maxRetryCount;
    private Duration retryDelay;

    public TempFileManager(int maxRetryCount, Duration retryDelay) {
        this(new DefaultFileSystem(), maxRetryCount, retryDelay);
    }

    /**
     * For testing purposes.
     */
    public TempFileManager(FileSystem fileSystem, int maxRetryCount, Duration retryDelay) {
        this.fileSystem = fileSystem;
        this.maxRetryCount = maxRetryCount;
        this.retryDelay = retryDelay;
    }

    public Path createTempFile(String prefix, String suffix) throws IOException {
        return fileSystem.createTempFile(prefix, suffix);
    }

    public void deleteAsync(Path path) {
        if (path == null) {
            return;
        }
        DELETE_EXECUTOR.submit(() -> deleteWithRetry(path));
    }

    private void deleteWithRetry(Path path) {
        int attempts = 0;

        while (attempts < maxRetryCount) {
            try {
                if (fileSystem.notExists(path)) {
                    return;
                }

                fileSystem.delete(path);
                return;
            } catch (IOException e) {
                attempts++;
                try {
                    TimeUnit.MILLISECONDS.sleep(retryDelay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (attempts >= maxRetryCount) {
            System.err.println("Failed to delete temp file: " + path + " after " + maxRetryCount + " attempts.");
        }
    }
}