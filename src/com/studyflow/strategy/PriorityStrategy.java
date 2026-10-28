package com.studyflow.strategy;

import com.studyflow.model.Task;

import java.time.LocalDate;

/**
 * Strategy interface: each implementation is one way of deciding how
 * important a task is. The rest of the system depends only on this interface,
 * so new rules can be added without changing existing code (Open/Closed).
 */
public interface PriorityStrategy {

    /**
     * Score from 0 to 100.
     *
     * @param remainingHours hours of work still to do; passed explicitly so the
     *                       scheduler can ask "what if I had already planned X hours?"
     *                       without changing the task itself.
     */
    double calculate(Task task, LocalDate today, double remainingHours);

    /** Convenience overload using the task's real remaining hours. */
    default double calculate(Task task, LocalDate today) {
        return calculate(task, today, task.getRemainingHours());
    }

    String getName();

    String getDescription();
}
