package com.studyflow.model;

/** Anything whose completion can be measured (tasks and subjects). */
public interface Trackable {
    /** Progress from 0 to 100. */
    double getProgressPercent();

    boolean isCompleted();
}
