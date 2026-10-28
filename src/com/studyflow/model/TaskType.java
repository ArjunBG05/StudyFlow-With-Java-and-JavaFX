package com.studyflow.model;

public enum TaskType {
    ASSIGNMENT("Assignment"),
    EXAM("Exam"),
    PROJECT("Project");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
