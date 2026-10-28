package com.studyflow.model;

/** Human-friendly band for a numeric priority score. */
public enum PriorityLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String label;

    PriorityLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PriorityLevel fromScore(double score) {
        if (score >= 70) {
            return CRITICAL;
        }
        if (score >= 50) {
            return HIGH;
        }
        if (score >= 30) {
            return MEDIUM;
        }
        return LOW;
    }
}
