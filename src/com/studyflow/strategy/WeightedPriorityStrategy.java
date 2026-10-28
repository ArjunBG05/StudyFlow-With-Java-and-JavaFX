package com.studyflow.strategy;

import com.studyflow.model.Task;

/**
 * Default strategy. Combines four factors:
 * urgency 50%, difficulty 15%, workload 15%, readiness gap 20% (exams only;
 * for other tasks the readiness share is added to urgency).
 * The result is multiplied by the task type's own weight (polymorphism).
 */
public class WeightedPriorityStrategy extends BasePriorityStrategy {

    private static final double W_URGENCY = 0.50;
    private static final double W_DIFFICULTY = 0.15;
    private static final double W_WORKLOAD = 0.15;
    private static final double W_READINESS = 0.20;

    @Override
    protected double rawScore(Task task, long daysLeft, double remainingHours) {
        double wUrgency = W_URGENCY;
        double wReadiness = 0.0;
        if (task.isReadinessSensitive()) {
            wReadiness = W_READINESS;
        } else {
            wUrgency += W_READINESS;
        }
        double raw = wUrgency * urgency(daysLeft, remainingHours)
                + W_DIFFICULTY * task.getDifficulty().getWeight()
                + W_WORKLOAD * workload(remainingHours)
                + wReadiness * task.getReadinessGap();
        return raw * task.getTypeWeight();
    }

    @Override
    public String getName() {
        return "Balanced (default)";
    }

    @Override
    public String getDescription() {
        return "Mixes deadline, difficulty, workload and (for exams) how unprepared you are";
    }
}
