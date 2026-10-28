package com.studyflow.exception;

/**
 * Unchecked exception for invalid arguments (blank names, out-of-range
 * values, duplicates). Unchecked so that constructors and setters stay clean.
 */
public class InvalidInputException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
