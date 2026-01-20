package com.baghajanyan.sandbox.core.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * waits up to {@code terminationTimeout} for pending tasks to finish and logs a
 * warning if tasks remain after the timeout.</li>
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
 * <li>Asynchronous deletion attempts to delete the file with retry semantics
 * up to {@code maxRetryCount} attempts, waiting {@code retryDelay} between
 * attempts.</li>
 * <li>If a deletion task is interrupted, it logs a warning and stops.</li>
 * <li>Because the executor uses non-daemon threads, {@link #close()} may
 * block the JVM shutdown for up to the timeout to allow deletions to
 * finish.</li>
 * </ul>
 */
public class TempFileManager implements AutoCloseable {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    private final FileSystem fileSystem;
    private final int maxRetryCount;
    private final Duration retryDelay;
    private final Duration terminationTimeout;
    private final ExecutorService deleteExecutor;
    private final Logger logger;

    /**
     * Creates a {@link TempFileManager} with default FileSystem.
     * 
     * @param maxRetryCount      The maximum number of retry attempts for file
     *                           deletion.
     * @param retryDelay         The {@link Duration} between retry attempts.
     * @param terminationTimeout The {@link Duration} to wait for pending deletions
     */
    public TempFileManager(int maxRetryCount, Duration retryDelay, Duration terminationTimeout) {
        this(new DefaultFileSystem(), maxRetryCount, retryDelay, terminationTimeout);
    }

    /**
     * Creates a {@link TempFileManager} with the specified FileSystem.
     * 
     * @param fileSystem         The {@link FileSystem} implementation to use.
     * @param maxRetryCount      The maximum number of retry attempts for file
     *                           deletion.
     * @param retryDelay         The {@link Duration} between retry attempts.
     * @param terminationTimeout The {@link Duration} to wait for pending deletions
     */
    public TempFileManager(FileSystem fileSystem, int maxRetryCount, Duration retryDelay, Duration terminationTimeout) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem cannot be null");
        if (maxRetryCount < 1) {
            throw new IllegalArgumentException("maxRetryCount must be positive");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("retryDelay must be positive");
        }
        if (terminationTimeout == null || terminationTimeout.isNegative() || terminationTimeout.isZero()) {
            throw new IllegalArgumentException("terminationTimeout must be positive");
        }

        this.maxRetryCount = maxRetryCount;
        this.retryDelay = retryDelay;
        this.terminationTimeout = terminationTimeout;

        this.deleteExecutor = Executors.newSingleThreadExecutor(backgroundThreadFactory());
        this.logger = Logger.getLogger(TempFileManager.class.getName());
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
     * @param fileSystem         The {@link FileSystem} implementation to use.
     * @param maxRetryCount      The maximum number of retry attempts for file
     *                           deletion.
     * @param retryDelay         The {@link Duration} between retry attempts.
     * @param terminationTimeout The {@link Duration} to wait for pending deletions
     * @param deleteExecutor     The {@link ExecutorService} to use for asynchronous
     *                           deletions (will be shut down on close)
     * @throws NullPointerException     if {@code fileSystem} or
     *                                  {@code deleteExecutor} is {@code null}
     * @throws IllegalArgumentException if {@code maxRetryCount} &lt; 1 or
     *                                  {@code retryDelay} is null or non-positive
     */
    public TempFileManager(FileSystem fileSystem, int maxRetryCount, Duration retryDelay, Duration terminationTimeout,
            ExecutorService deleteExecutor) {
        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem cannot be null");
        if (maxRetryCount < 1) {
            throw new IllegalArgumentException("maxRetryCount must be positive");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("retryDelay must be positive");
        }
        if (terminationTimeout == null || terminationTimeout.isNegative() || terminationTimeout.isZero()) {
            throw new IllegalArgumentException("terminationTimeout must be positive");
        }

        this.maxRetryCount = maxRetryCount;
        this.retryDelay = retryDelay;
        this.terminationTimeout = terminationTimeout;

        this.deleteExecutor = Objects.requireNonNull(deleteExecutor, "deleteExecutor cannot be null");
        this.logger = Logger.getLogger(TempFileManager.class.getName());
    }

    /**
     * Initiates an orderly shutdown of the deletion executor.
     *
     * <p>
     * Already submitted deletion tasks are allowed to complete. This method blocks
     * for up to {@code terminationTimeout} waiting for pending tasks to finish.
     * After the timeout, any unfinished tasks may remain and a warning is logged.
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
            boolean allDone = deleteExecutor.awaitTermination(terminationTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!allDone) {
                logger.warning(
                        "Not all temp files were deleted within %s seconds".formatted(terminationTimeout.toSeconds()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while waiting for deletions to finish");
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
        for (int attempt = 1; attempt <= maxRetryCount; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                logger.warning(() -> "Delete task interrupted for " + path);
                return;
            }

            try {
                if (fileSystem.notExists(path)) {
                    return;
                }

                fileSystem.delete(path);
                return;

            } catch (IOException e) {
                logger.log(Level.WARNING,
                        "Failed to delete temp file (attempt {0}/{1}, retryDelay={2}ms, error={3}): {4}",
                        new Object[] { attempt, maxRetryCount, retryDelay.toMillis(), e.getMessage(), path });

                sleepBeforeRetry();
            }
        }

        logger.severe(() -> "Giving up deleting temp file after " + maxRetryCount + " attempts: " + path);
    }

    private void sleepBeforeRetry() {
        try {
            TimeUnit.MILLISECONDS.sleep(retryDelay.toMillis());
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