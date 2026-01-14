# sandbox-core

Language-agnostic sandbox library for executing code snippets and managing temporary files.

## Features

- CodeExecutor interface for language-agnostic code execution.
- TempFileManager for robust temporary file management with retry mechanism.
- ExecutionResult and CodeSnippet models for clear data representation.
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

Add as Maven dependency (after building locally):

```xml
<dependency>
  <groupId>com.baghajanyan</groupId>
  <artifactId>sandbox-core</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### TempFileManager Example

```java
import com.baghajanyan.sandbox.core.fs.TempFileManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

public class TempFileExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a TempFileManager with 3 retry attempts and a 100ms delay.
        var tempFileManager = new TempFileManager(3, Duration.ofMillis(100));

        // Create a temporary file.
        Path tempFile = tempFileManager.createTempFile("my-temp-file", ".txt");
        System.out.println("Created temporary file: " + tempFile);

        // Asynchronously delete the file.
        tempFileManager.deleteAsync(tempFile);
        System.out.println("Asynchronously deleting file...");

        // Give some time for the async deletion to complete.
        Thread.sleep(500);

        System.out.println("File deleted.");
    }
}
```

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.

## License

This project is licensed under the MIT License.

