package com.studyflow.strategy;

import com.studyflow.model.PriorityLevel;
import com.studyflow.model.Task;

/** Immutable pairing of a task with its computed score. */
public final class RankedTask {

    private final Task task;
    private final double score;

    public RankedTask(Task task, double score) {
        this.task = task;
        this.score = score;
    }

    public Task getTask() {
        return task;
    }

    public double getScore() {
        return score;
    }

    public PriorityLevel getLevel() {
        return PriorityLevel.fromScore(score);
    }

    @Override
    public String toString() {
        return String.format("%.1f %s - %s", score, getLevel().getLabel(), task);
    }
}
