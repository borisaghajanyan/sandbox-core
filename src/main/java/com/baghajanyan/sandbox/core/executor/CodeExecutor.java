package com.baghajanyan.sandbox.core.executor;

import com.baghajanyan.sandbox.core.model.CodeSnippet;

public interface CodeExecutor {
    /**
     * Executes the given code snippet and returns the result of the execution.
     * 
     * @param snippet The code snippet to be executed.
     * @return An ExecutionResult containing details about the execution.
     */
    ExecutionResult execute(CodeSnippet snippet);
}
