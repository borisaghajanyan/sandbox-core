package com.baghajanyan.sandbox.core.model;

import java.time.Duration;

/**
 * Represents a code snippet with its associated programming language.
 * 
 * @param code     The actual code snippet as a string.
 * @param timeout  The maximum execution time allowed for the code snippet.
 * @param language The programming language of the code snippet (e.g., "java",
 *                 "python").
 */
public record CodeSnippet(String code, Duration timeout, String language) {

}
