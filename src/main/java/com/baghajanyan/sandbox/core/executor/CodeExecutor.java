package com.baghajanyan.sandbox.core.executor;

import com.baghajanyan.sandbox.core.model.CodeSnippet;

public interface CodeExecutor {
    ExecutionResult execute(CodeSnippet snippet);
}
