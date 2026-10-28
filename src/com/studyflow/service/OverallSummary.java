package com.studyflow.service;

import java.util.Optional;

/** Cross-subject statistics for the dashboard. */
public final class OverallSummary {

    private final int totalTasks;
    private final int completedTasks;
    private final int overdueTasks;
    private final double totalHours;
    private final double hoursLast7Days;
    private final String focusSubject;

    public OverallSummary(int totalTasks, int completedTasks, int overdueTasks,
                          double totalHours, double hoursLast7Days, String focusSubject) {
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.overdueTasks = overdueTasks;
        this.totalHours = totalHours;
        this.hoursLast7Days = hoursLast7Days;
        this.focusSubject = focusSubject;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getHoursLast7Days() {
        return hoursLast7Days;
    }

    public double getCompletionRate() {
        return totalTasks == 0 ? 0.0 : 100.0 * completedTasks / totalTasks;
    }

    /** The subject that most deserves attention right now, if any. */
    public Optional<String> getFocusSubject() {
        return Optional.ofNullable(focusSubject);
    }
}
