package com.baghajanyan.sandbox.core.fs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TempFileManagerTest {
    private TempFileManager tempFileManager;

    @Mock
    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        tempFileManager = new TempFileManager(fileSystem, 3, Duration.ofMillis(10));
    }

    @Test
    void create() throws IOException {
        var mockFile = Path.of("mock-file.tmp");
        when(fileSystem.createTempFile(any(), any())).thenReturn(mockFile);
        var file = tempFileManager.createTempFile("file", ".tmp");
        assertTrue(file.equals(mockFile));
    }

    @Test
    void deleteAsync() throws IOException, InterruptedException {
        var mockFile = Path.of("mock-file.tmp");
        tempFileManager.deleteAsync(mockFile);
        Thread.sleep(Duration.ofMillis(100).toMillis());
        verify(fileSystem, times(1)).delete(mockFile);
    }

    @Test
    void deleteAsync_whenFileDoesNotExist_doNothing() throws IOException, InterruptedException {
        var nonExistent = Path.of("non-existent-file.tmp");
        when(fileSystem.notExists(nonExistent)).thenReturn(true);
        assertDoesNotThrow(() -> tempFileManager.deleteAsync(nonExistent));
        Thread.sleep(Duration.ofMillis(100).toMillis());
        verify(fileSystem, times(0)).delete(nonExistent);
    }

    @Test
    void deleteWithRetry_whenIoException_shouldRetry() throws IOException, InterruptedException {
        var mockFile = Path.of("mock-file.tmp");
        when(fileSystem.notExists(mockFile)).thenReturn(false);
        doThrow(new IOException("Failed to delete"))
                .doThrow(new IOException("Failed to delete"))
                .doNothing()
                .when(fileSystem).delete(mockFile);

        tempFileManager.deleteAsync(mockFile);
        Thread.sleep(Duration.ofMillis(100).toMillis());
        verify(fileSystem, times(3)).delete(mockFile);
    }
}
