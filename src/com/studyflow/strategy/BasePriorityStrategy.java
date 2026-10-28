package com.studyflow.strategy;

import com.studyflow.model.Task;
import com.studyflow.util.NumberUtil;

import java.time.LocalDate;

/**
 * Abstract class implementing the parts every strategy shares (Template
 * Method): ignore finished work, boost overdue work, clamp to 0-100 and
 * round. Subclasses only provide rawScore().
 */
public abstract class BasePriorityStrategy implements PriorityStrategy {

    protected static final double OVERDUE_BONUS = 15.0;

    @Override
    public final double calculate(Task task, LocalDate today, double remainingHours) {
        if (!task.isActive() || remainingHours <= 0) {
            return 0.0;
        }
        long days = task.daysLeft(today);
        double score = 100.0 * rawScore(task, days, remainingHours);
        if (days < 0) {
            score += OVERDUE_BONUS;
        }
        return NumberUtil.round1(Math.max(0.0, Math.min(100.0, score)));
    }

    /** Returns roughly 0..1 (may slightly exceed 1 before clamping). */
    protected abstract double rawScore(Task task, long daysLeft, double remainingHours);

    /** 1.0 when due or overdue, decaying with time; rises again if the workload per day is heavy. */
    protected static double urgency(long daysLeft, double remainingHours) {
        if (daysLeft <= 0) {
            return 1.0;
        }
        double u = 1.0 / (1.0 + 0.35 * daysLeft);
        double hoursPerDay = remainingHours / daysLeft;
        u += Math.max(0.0, hoursPerDay - 3.0) * 0.1;
        return Math.min(1.0, u);
    }

    protected static double workload(double remainingHours) {
        return Math.min(1.0, remainingHours / 12.0);
    }
}
