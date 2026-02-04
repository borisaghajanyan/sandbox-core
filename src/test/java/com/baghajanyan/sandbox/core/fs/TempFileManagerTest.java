package com.baghajanyan.sandbox.core.fs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TempFileManagerTest {

    @TempDir
    Path tempDir;

    private TempFileManager newManager() {
        return new TempFileManager(new DefaultFileSystem(),
                new DeleteConfig(5, Duration.ofMillis(50), Duration.ofSeconds(2)));
    }

    private TempFileManager newManager(FileSystem fs, Duration terminationTimeout) {
        return new TempFileManager(fs, new DeleteConfig(5, Duration.ofMillis(50), terminationTimeout));
    }

    @Test
    void constructor_shouldValidateParameters() {
        assertAll("Constructor edge cases",
                () -> assertThrows(NullPointerException.class,
                        () -> new TempFileManager(null,
                                new DeleteConfig(5, Duration.ofMillis(50), Duration.ofSeconds(2)))),
                () -> assertThrows(NullPointerException.class,
                        () -> new TempFileManager(new DefaultFileSystem(), null)));
    }

    @Test
    void createTempFile() throws IOException {
        try (TempFileManager manager = newManager()) {
            Path file = manager.createTempFile("test", ".txt");
            assertTrue(Files.exists(file));

            Files.deleteIfExists(file);
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void write() throws IOException {
        try (TempFileManager manager = newManager()) {
            Path file = Files.createTempFile(tempDir, "write", ".txt");

            manager.write(file, "hello");
            assertEquals("hello", Files.readString(file));

            Files.deleteIfExists(file);
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void delete() throws IOException {
        try (TempFileManager manager = newManager()) {
            Path file = Files.createTempFile(tempDir, "sync", ".txt");
            manager.delete(file);

            assertFalse(Files.exists(file));
        }
    }

    @Test
    void delete_whenPathIsNull_throwsNullPointerException() {
        try (TempFileManager manager = newManager()) {
            assertThrows(NullPointerException.class, () -> manager.delete(null));
        }
    }

    @Test
    void deleteAsync() throws Exception {
        try (TempFileManager manager = newManager()) {
            Path file = Files.createTempFile(tempDir, "async", ".txt");

            Future<?> future = manager.deleteAsync(file);
            future.get(1, TimeUnit.SECONDS);

            assertFalse(Files.exists(file));
        }
    }

    @Test
    void deleteAsync_multipleFiles() throws Exception {
        try (TempFileManager manager = newManager()) {
            Path file1 = Files.createTempFile(tempDir, "async1", ".txt");
            Path file2 = Files.createTempFile(tempDir, "async2", ".txt");
            Path file3 = Files.createTempFile(tempDir, "async3", ".txt");

            Future<?> f1 = manager.deleteAsync(file1);
            Future<?> f2 = manager.deleteAsync(file2);
            Future<?> f3 = manager.deleteAsync(file3);

            f1.get(1, TimeUnit.SECONDS);
            f2.get(1, TimeUnit.SECONDS);
            f3.get(1, TimeUnit.SECONDS);

            assertFalse(Files.exists(file1));
            assertFalse(Files.exists(file2));
            assertFalse(Files.exists(file3));
        }
    }

    @Test
    void deleteAsync_whenPathIsNull_throwsNullPointerException() {
        try (TempFileManager manager = newManager()) {
            assertThrows(NullPointerException.class, () -> manager.deleteAsync(null));
        }
    }

    @Test
    void deleteAsync_retryUntilSuccess() throws Exception {
        Path file = tempDir.resolve("file.txt");
        FileSystem mockFs = mock(FileSystem.class);

        when(mockFs.notExists(file)).thenReturn(false);
        doThrow(new IOException("fail 1"))
                .doThrow(new IOException("fail 2"))
                .doNothing()
                .when(mockFs).delete(file);

        try (TempFileManager manager = newManager(mockFs, Duration.ofSeconds(2))) {
            Future<?> future = manager.deleteAsync(file);
            future.get(2, TimeUnit.SECONDS);
            verify(mockFs, times(3)).delete(file);
        }
    }

    @Test
    void close_waitForDeletionToFinish() throws Exception {
        Path file = tempDir.resolve("file.txt");

        FileSystem mockFs = mock(FileSystem.class);
        when(mockFs.notExists(file)).thenReturn(false);

        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(mockFs).delete(file);

        TempFileManager manager = newManager(mockFs, Duration.ofSeconds(1));
        manager.deleteAsync(file);

        long start = System.currentTimeMillis();
        manager.close();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 200);

        verify(mockFs, atLeastOnce()).delete(file);
    }

    @Test
    void close_timeoutIfDeleteNeverCompletes() throws Exception {
        Path file = tempDir.resolve("file.txt");

        FileSystem mockFs = mock(FileSystem.class);
        when(mockFs.notExists(file)).thenReturn(false);

        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(mockFs).delete(file);

        TempFileManager manager = newManager(mockFs, Duration.ofMillis(100));
        manager.deleteAsync(file);

        long start = System.currentTimeMillis();
        manager.close();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed <= 150); // may take slightly longer than 100ms

        verify(mockFs, times(1)).delete(file);
    }

    @Test
    void deleteAsync_afterClose_rejectNewTasks() {
        TempFileManager manager = newManager();
        manager.close();

        assertThrows(
                RejectedExecutionException.class,
                () -> manager.deleteAsync(tempDir.resolve("rejected.txt")));
    }
}
