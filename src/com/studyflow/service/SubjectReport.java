package com.studyflow.service;

import java.time.LocalDate;
import java.util.Optional;

/** Immutable snapshot of how one subject is going. */
public final class SubjectReport {

    private final String subjectName;
    private final double progress;
    private final double hoursStudied;
    private final long activeTasks;
    private final long completedTasks;
    private final long overdueTasks;
    private final LocalDate nextDeadline;
    private final double averagePriority;
    private final int preparation;
    private final HealthStatus status;

    public SubjectReport(String subjectName, double progress, double hoursStudied, long activeTasks,
                         long completedTasks, long overdueTasks, LocalDate nextDeadline,
                         double averagePriority, int preparation, HealthStatus status) {
        this.subjectName = subjectName;
        this.progress = progress;
        this.hoursStudied = hoursStudied;
        this.activeTasks = activeTasks;
        this.completedTasks = completedTasks;
        this.overdueTasks = overdueTasks;
        this.nextDeadline = nextDeadline;
        this.averagePriority = averagePriority;
        this.preparation = preparation;
        this.status = status;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public double getProgress() {
        return progress;
    }

    public double getHoursStudied() {
        return hoursStudied;
    }

    public long getActiveTasks() {
        return activeTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getOverdueTasks() {
        return overdueTasks;
    }

    public Optional<LocalDate> getNextDeadline() {
        return Optional.ofNullable(nextDeadline);
    }

    public double getAveragePriority() {
        return averagePriority;
    }

    public int getPreparation() {
        return preparation;
    }

    public HealthStatus getStatus() {
        return status;
    }
}
