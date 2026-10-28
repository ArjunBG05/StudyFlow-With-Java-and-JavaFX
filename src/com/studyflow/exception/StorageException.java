package com.studyflow.exception;

/** Thrown when saving or loading data from disk fails. */
public class StorageException extends StudyFlowException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
