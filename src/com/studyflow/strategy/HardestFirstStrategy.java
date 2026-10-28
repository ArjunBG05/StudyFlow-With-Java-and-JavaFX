package com.studyflow.strategy;

import com.studyflow.model.Task;

/** Tackle the toughest, biggest work early while energy is high. */
public class HardestFirstStrategy extends BasePriorityStrategy {

    @Override
    protected double rawScore(Task task, long daysLeft, double remainingHours) {
        return 0.6 * task.getDifficulty().getWeight()
                + 0.2 * workload(remainingHours)
                + 0.2 * urgency(daysLeft, remainingHours);
    }

    @Override
    public String getName() {
        return "Hardest first";
    }

    @Override
    public String getDescription() {
        return "Difficult and large tasks are started first";
    }
}
