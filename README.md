# sandbox-core

Language-agnostic sandbox library for executing code snippets and managing temporary files.

## Requirements

- Java 21

## Features

- **CodeExecutor** interface for language-agnostic code execution.
- **TempFileManager** for robust temporary file management with retry mechanism.
- **ExecutionResult** and **CodeSnippet** models for clear data representation.
- Fully testable with a decoupled, interface-based architecture.

## Modules

- **executor**: Code execution engine.
- **model**: Data models for code snippets and execution results.
- **fs**: File system utilities, including the TempFileManager.

## Build

To build the project, run the following command:

```bash
./mvnw clean install
```

## Usage

### JitPack

You can consume this library directly from JitPack.

1. Add the JitPack repository:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

1. Add the dependency:

```xml
<dependency>
  <groupId>com.github.borisaghajanyan</groupId>
  <artifactId>sandbox-core</artifactId>
  <version>main-SNAPSHOT</version>
</dependency>
```

### TempFileManager Example

The following example demonstrates how to create a temporary file and wait for its asynchronous deletion to complete.

```java
import com.baghajanyan.sandbox.core.fs.TempFileManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TempFileExample {
    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // Use a try-with-resources block to ensure the TempFileManager is closed properly.
        // The manager uses a non-daemon delete thread, so close it to avoid blocking JVM shutdown.
        // This manager has 3 retry attempts with a 100ms delay, and a 5-second shutdown timeout.
        var deleteConfig = new DeleteConfig(3, Duration.ofMillis(100), Duration.ofSeconds(5));
        try (var tempFileManager = new TempFileManager(deleteConfig)) {
            // Create a temporary file.
            Path tempFile = tempFileManager.createTempFile("my-temp-file", ".txt");
            System.out.println("Created temporary file: " + tempFile);

            // Asynchronously delete the file and get the Future.
            Future<?> deleteFuture = tempFileManager.deleteAsync(tempFile);
            System.out.println("Asynchronously deleting file...");

            // Block and wait for the deletion to complete (up to a timeout).
            // This is more reliable than Thread.sleep().
            deleteFuture.get(1, TimeUnit.SECONDS);

            System.out.println("File deletion confirmed.");
        }
    }
}
```

## Logging

This library uses the [SLF4J](https://www.slf4j.org/) API for logging. This means you can choose your own logging framework (like `Logback`, `Log4j2`, or `java.util.logging`) by adding the appropriate SLF4J binding to your project. The library does not force a specific logging implementation on you.

For example, to use `Logback`, you would add the `logback-classic` dependency to your `pom.xml`.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.

## License

This project is licensed under the MIT License.
