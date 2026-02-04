package com.baghajanyan.sandbox.core.executor;

import java.time.Duration;

/**
 * Represents the result of executing a code snippet.
 * 
 * @param exitCode      The exit code of the execution process, can be different
 *                      values depending on the execution environment.
 * @param stdout        The standard output produced by the execution.
 * @param stderr        The standard error output produced by the execution.
 * @param executionTime The time taken to execute the code snippet.
 */
public record ExecutionResult(int exitCode, String stdout, String stderr, Duration executionTime) {

}
