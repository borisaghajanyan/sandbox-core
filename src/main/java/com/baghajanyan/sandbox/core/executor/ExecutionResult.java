package com.baghajanyan.sandbox.core.executor;

public record ExecutionResult(int exitCode, String stdout, String stderr) {

}
