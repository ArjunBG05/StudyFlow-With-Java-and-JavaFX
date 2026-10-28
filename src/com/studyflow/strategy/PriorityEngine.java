package com.studyflow.strategy;

import com.studyflow.model.Task;
import com.studyflow.util.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Context class of the Strategy pattern. It holds the current strategy and
 * can be switched at run time.
 */
public class PriorityEngine {

    private PriorityStrategy strategy;

    public PriorityEngine(PriorityStrategy strategy) {
        setStrategy(strategy);
    }

    public PriorityStrategy getStrategy() {
        return strategy;
    }

    public final void setStrategy(PriorityStrategy strategy) {
        this.strategy = Validator.requireNonNull(strategy, "Priority strategy");
    }

    public double score(Task task, LocalDate today) {
        return strategy.calculate(task, today);
    }

    /** Active tasks sorted from most to least important (ties: earlier deadline first). */
    public List<RankedTask> rank(Collection<Task> tasks, LocalDate today) {
        return tasks.stream()
                .filter(Task::isActive)
                .map(t -> new RankedTask(t, strategy.calculate(t, today)))
                .sorted(Comparator.comparingDouble(RankedTask::getScore).reversed()
                        .thenComparing(rt -> rt.getTask().getDueDate()))
                .collect(Collectors.toList());
    }

    public static List<PriorityStrategy> availableStrategies() {
        return Arrays.asList(new WeightedPriorityStrategy(),
                new DeadlineFirstStrategy(), new HardestFirstStrategy());
    }
}
