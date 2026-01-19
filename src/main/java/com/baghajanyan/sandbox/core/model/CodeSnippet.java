package com.baghajanyan.sandbox.core.model;

/**
 * Represents a code snippet with its associated programming language.
 * 
 * @param code     The actual code snippet as a string.
 * @param timeout  The maximum execution time allowed for the code snippet in
 *                 milliseconds.
 * @param language The programming language of the code snippet (e.g., "java",
 *                 "python").
 */
public record CodeSnippet(String code, long timeout, String language) {

}
