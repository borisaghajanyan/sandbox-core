package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages creation and cleanup of temporary files.
 *
 * <p>
 * This class supports both synchronous and asynchronous deletion of files.
 * Asynchronous deletions are executed on a single background thread and
 * use retry semantics. The shutdown process will wait up to a fixed timeout
 * for pending deletion tasks to complete.
 * </p>
 *
 * <h2>Threading and Lifecycle</h2>
 * <ul>
 * <li>Asynchronous deletions are executed using a single-threaded
 * {@link ExecutorService} backed by a <b>non-daemon thread</b>.</li>
 * <li>Non-daemon threads block JVM shutdown until tasks finish or the
 * {@link #close()} timeout elapses.</li>
 * <li>For testing, a custom executor may be injected, which can use
 * alternative threading or timeouts.</li>
 * </ul>
 *
 * <h2>Resource Management</h2>
 * <ul>
 * <li>This class owns its executor and must be closed when no longer
 * needed.</li>
 * <li>Calling {@link #close()} initiates an orderly shutdown of the executor,
 * waits up to {@code deleteConfig.terminationTimeout()} for pending tasks to
 * finish and logs a warning if tasks remain after the timeout.</li>
 * <li>Submitting tasks after {@code close()} may result in
 * {@link java.util.concurrent.RejectedExecutionException}.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe provided that the supplied {@link FileSystem}
 * implementation is thread-safe.
 * </p>
 *
 * <h2>Behavior Notes</h2>
 * <ul>
 * <li>Synchronous deletion is guaranteed to either succeed or throw an
 * IOException.</li>
 * <li>Asynchronous deletion attempts to delete the file with retry semantics up
 * to {@code deleteConfig.maxRetries()} attempts, waiting
 * {@code deleteConfig.retryDelay()} between attempts.</li>
 * <li>If a deletion task is interrupted, it logs a warning and stops.</li>
 * <li>Because the executor uses non-daemon threads, {@link #close()} may
 * block the JVM shutdown for up to the timeout to allow deletions to
 * finish.</li>
 * </ul>
 */
public class TempFileManager implements AutoCloseable {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    private static final Logger logger = LoggerFactory.getLogger(TempFileManager.class);

    private final FileSystem fileSystem;
    private final DeleteConfig deleteConfig;
    private final ExecutorService deleteExecutor;

    /**
     * Creates a {@link TempFileManager} with default FileSystem.
     * 
     * @param deleteConfig The {@link DeleteConfig} to use for deletion.
     */
    public TempFileManager(DeleteConfig deleteConfig) {
        this(new DefaultFileSystem(), deleteConfig);
    }

    /**
     * Creates a {@link TempFileManager} with the specified FileSystem.
     * 
     * @param fileSystem   The {@link FileSystem} implementation to use.
     * @param deleteConfig The {@link DeleteConfig} to use for deletion.
     * 
     * @throws NullPointerException if {@code fileSystem} or
     *                              {@code deleteConfig} is {@code null}
     */
    public TempFileManager(FileSystem fileSystem, DeleteConfig deleteConfig) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem cannot be null");
        this.deleteConfig = Objects.requireNonNull(deleteConfig, "deleteConfig cannot be null");
        this.deleteExecutor = Executors.newSingleThreadExecutor(backgroundThreadFactory());
    }

    /**
     * Creates a {@link TempFileManager} with a custom ExecutorService.
     *
     * <p>
     * This constructor is intended for testing purposes only. The provided
     * {@link ExecutorService} will be used for asynchronous deletion and will
     * be shut down when {@link #close()} is called.
     * </p>
     *
     * @param fileSystem     The {@link FileSystem} implementation to use.
     * @param deleteConfig   The {@link DeleteConfig} to use for deletion.
     * @param deleteExecutor The {@link ExecutorService} to use for asynchronous
     *                       deletions (will be shut down on close)
     * @throws NullPointerException if {@code fileSystem} or
     *                              {@code deleteExecutor} is {@code null}
     */
    public TempFileManager(FileSystem fileSystem, DeleteConfig deleteConfig, ExecutorService deleteExecutor) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem cannot be null");
        this.deleteConfig = Objects.requireNonNull(deleteConfig, "deleteConfig cannot be null");
        this.deleteExecutor = Objects.requireNonNull(deleteExecutor, "deleteExecutor cannot be null");
    }

    /**
     * Initiates an orderly shutdown of the deletion executor.
     *
     * <p>
     * Already submitted deletion tasks are allowed to complete. This method blocks
     * for up to {@code deleteConfig.terminationTimeout()} waiting for pending tasks
     * to finish. After the timeout, any unfinished tasks may remain and a warning
     * is logged.
     * </p>
     *
     * <p>
     * Because the executor uses non-daemon threads, this method also ensures
     * that the JVM will wait for pending deletions up to the timeout before
     * exiting.
     * </p>
     */
    @Override
    public void close() {
        deleteExecutor.shutdown();
        try {
            boolean allDone = deleteExecutor.awaitTermination(deleteConfig.terminationTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            if (!allDone) {
                logger.warn("Not all temp files were deleted within {} seconds",
                        deleteConfig.terminationTimeout().toSeconds());
                deleteExecutor.shutdownNow();
                boolean forcedDone = deleteExecutor.awaitTermination(deleteConfig.terminationTimeout().toMillis(),
                        TimeUnit.MILLISECONDS);
                if (!forcedDone) {
                    logger.warn("Deletion executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for deletions to finish");
        }
    }

    public Path createTempFile(String prefix, String suffix) throws IOException {
        return fileSystem.createTempFile(prefix, suffix);
    }

    public void write(Path path, String content) throws IOException {
        fileSystem.write(path, content);
    }

    /**
     * Deletes the given file synchronously.
     *
     * @param path the file to delete (must not be {@code null})
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if deletion fails
     */
    public void delete(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        fileSystem.delete(path);
    }

    /**
     * Deletes the given file asynchronously using retry semantics.
     *
     * <p>
     * The deletion task is executed on a non-daemon thread. If the JVM shuts down,
     * the shutdown process will wait for pending tasks up to the {@link #close()}
     * timeout.
     * </p>
     *
     * @param path the file to delete (must not be {@code null})
     * @return a {@link Future} representing the deletion task
     * @throws NullPointerException       if {@code path} is {@code null}
     * @throws RejectedExecutionException if the manager has been closed
     */
    public Future<?> deleteAsync(Path path) {
        Objects.requireNonNull(path, "path");
        return deleteExecutor.submit(() -> deleteWithRetry(path));
    }

    private void deleteWithRetry(Path path) {
        for (int attempt = 1; attempt <= deleteConfig.maxRetries(); attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                logger.warn("Delete task interrupted for {}", path);
                return;
            }

            try {
                if (fileSystem.notExists(path)) {
                    return;
                }

                fileSystem.delete(path);
                return;

            } catch (IOException e) {
                logger.warn("Failed to delete temp file (attempt {}/{}, retryDelay={}ms, error={}): {}",
                        attempt, deleteConfig.maxRetries(), deleteConfig.retryDelay().toMillis(), e.getMessage(), path);

                sleepBeforeRetry();
            }
        }

        logger.error("Giving up deleting temp file after {} attempts: {}", deleteConfig.maxRetries(), path);
    }

    private void sleepBeforeRetry() {
        try {
            TimeUnit.MILLISECONDS.sleep(deleteConfig.retryDelay().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return;
        }
    }

    /**
     * Creates a ThreadFactory that produces background (non-daemon) threads
     * for executing deletion tasks. Each thread has a unique name for logging
     * purposes.
     *
     * @return a ThreadFactory producing non-daemon threads
     */
    private static ThreadFactory backgroundThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "temp-file-delete-thread -" + THREAD_COUNT.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
    }
}
