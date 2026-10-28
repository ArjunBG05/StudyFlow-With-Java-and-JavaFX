package com.studyflow.service;

public enum HealthStatus {
    NO_TASKS("No tasks"),
    ON_TRACK("On track"),
    NEEDS_ATTENTION("Needs attention"),
    AT_RISK("At risk");

    private final String label;

    HealthStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
