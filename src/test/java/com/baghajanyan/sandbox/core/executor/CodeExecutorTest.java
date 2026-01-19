package com.baghajanyan.sandbox.core.executor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.baghajanyan.sandbox.core.model.CodeSnippet;

class DummyExecutor implements CodeExecutor {
    @Override
    public ExecutionResult execute(CodeSnippet snippet) {
        return new ExecutionResult(0, "Dummy Output", "", 1L);
    }
}

public class CodeExecutorTest {
    @Test
    void dummyExecutor() {
        var executor = new DummyExecutor();
        var result = executor.execute(new CodeSnippet("echo 'Hello World!';", 1000L, "php"));
        assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals("Dummy Output", result.stdout()),
                () -> assertEquals("Dummy Output", result.stdout()),
                () -> assertEquals(1L, result.executionTime()));
    }
}
