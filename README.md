# sandbox-core

Language-agnostic sandbox library for executing code snippets and storing in-memory data.

## Features

- CodeExecutor interface
- ExecutionResult model
- CodeSnippet model
- DataStore interface
- Fully testable

## Modules

- **datastore**: In-memory data storage
- **executor**: Code execution engine
- **model**: Data models for code snippets

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

### Example

```java
import com.baghajanyan.sandbox.core.executor.CodeExecutor;
import com.baghajanyan.sandbox.core.executor.ExecutionResult;
import com.baghajanyan.sandbox.core.model.CodeSnippet;

class MyExecutor implements CodeExecutor {
    @Override
    public ExecutionResult execute(CodeSnippet snippet) {
        // Implement your execution logic here
        return new ExecutionResult(0, "Hello, " + snippet.code(), "");
    }
}

public class Main {
    public static void main(String[] args) {
        var executor = new MyExecutor();
        var result = executor.execute(new CodeSnippet("World", "text"));
        System.out.println(result.stdout()); // Hello, World
    }
}
```
