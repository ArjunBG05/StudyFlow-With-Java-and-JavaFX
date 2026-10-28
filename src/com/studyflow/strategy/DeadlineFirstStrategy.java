package com.studyflow.strategy;

import com.studyflow.model.Task;

/** Classic Earliest-Deadline-First: only the due date matters. */
public class DeadlineFirstStrategy extends BasePriorityStrategy {

    @Override
    protected double rawScore(Task task, long daysLeft, double remainingHours) {
        return 1.0 / (1.0 + Math.max(0, daysLeft));
    }

    @Override
    public String getName() {
        return "Deadline first";
    }

    @Override
    public String getDescription() {
        return "Whatever is due soonest comes first, ignoring difficulty and workload";
    }
}
