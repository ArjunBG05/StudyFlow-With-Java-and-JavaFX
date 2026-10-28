package com.studyflow.exception;

/**
 * Base class for recoverable (checked) application errors, e.g. a missing
 * entity or a storage failure. Callers are forced to handle these.
 */
public class StudyFlowException extends Exception {
    private static final long serialVersionUID = 1L;

    public StudyFlowException(String message) {
        super(message);
    }

    public StudyFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
