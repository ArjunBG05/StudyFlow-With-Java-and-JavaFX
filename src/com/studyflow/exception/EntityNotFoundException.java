package com.studyflow.exception;

/** Thrown when a subject, task or session cannot be found. */
public class EntityNotFoundException extends StudyFlowException {
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entity, String key) {
        super(entity + " not found: " + key);
    }
}
