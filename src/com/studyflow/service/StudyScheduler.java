package com.studyflow.service;

import com.studyflow.exception.InvalidInputException;
import com.studyflow.model.Task;
import com.studyflow.strategy.PriorityStrategy;
import com.studyflow.util.NumberUtil;
import com.studyflow.util.Validator;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Greedy, priority-driven planner.
 *
 * For every day it repeatedly picks the task with the highest priority
 * (re-scored with the hours already planned, via the "what-if" overload of the
 * strategy), gives it a block of at most maxBlock hours and carries on until the
 * day's capacity is used. Because the score falls as hours are allocated, work
 * is naturally interleaved across subjects instead of binge-studying one.
 * A task is never scheduled after its deadline; any hours that do not fit are
 * reported as "at risk".
 *
 * Feasibility safeguard: before each day is filled, a backward pass pushes every
 * task's work as late as its deadline allows (latest deadline first). Whatever
 * cannot be pushed later "must" be studied today, and those tasks are served
 * first. This stops a high-priority but distant task from stealing slots that a
 * nearer deadline needs, so a task is only flagged when it is genuinely impossible.
 */
public class StudyScheduler {

    private static final double EPS = 1e-9;

    private double dailyHours;
    private double maxBlockHours;

    public StudyScheduler(double dailyHours, double maxBlockHours) {
        setDailyHours(dailyHours);
        setMaxBlockHours(maxBlockHours);
    }

    public double getDailyHours() {
        return dailyHours;
    }

    public final void setDailyHours(double dailyHours) {
        Validator.requirePositive(dailyHours, "Daily study hours");
        if (dailyHours > 16) {
            throw new InvalidInputException("Daily study hours cannot exceed 16");
        }
        this.dailyHours = dailyHours;
    }

    public double getMaxBlockHours() {
        return maxBlockHours;
    }

    public final void setMaxBlockHours(double maxBlockHours) {
        this.maxBlockHours = Validator.requirePositive(maxBlockHours, "Maximum block length");
    }

    public Schedule build(Collection<Task> tasks, LocalDate start, int days, PriorityStrategy strategy) {
        Validator.requireRange(days, 1, 60, "Planning days");
        List<Task> active = tasks.stream().filter(Task::isActive).collect(Collectors.toList());
        Map<String, Double> remaining = new HashMap<>();
        for (Task t : active) {
            remaining.put(t.getId(), t.getRemainingHours());
        }

        Schedule schedule = new Schedule(start, days);
        for (int offset = 0; offset < days; offset++) {
            LocalDate day = start.plusDays(offset);
            double capacity = dailyHours;
            Map<String, Double> usedToday = new HashMap<>();
            Map<String, Double> mustLeft = mustDoToday(active, remaining, day);

            while (capacity > EPS) {
                Optional<Task> best = active.stream()
                        .filter(t -> remaining.get(t.getId()) > EPS)
                        .filter(t -> !t.getDueDate().isBefore(day))
                        .filter(t -> usedToday.getOrDefault(t.getId(), 0.0) < maxBlockHours - EPS)
                        .max(Comparator
                                .<Task>comparingInt(t -> mustLeft.getOrDefault(t.getId(), 0.0) > EPS ? 1 : 0)
                                .thenComparingDouble(t -> strategy.calculate(t, day, remaining.get(t.getId())))
                                .thenComparing(Comparator.comparing(Task::getDueDate).reversed()));
                if (!best.isPresent()) {
                    break;
                }
                Task task = best.get();
                double room = Math.min(maxBlockHours - usedToday.getOrDefault(task.getId(), 0.0),
                        Math.min(remaining.get(task.getId()), capacity));
                double block = Math.floor(room * 2) / 2;      // use half-hour slots where possible
                if (block < EPS) {
                    block = room;                              // a final sliver smaller than 30 minutes
                }
                final double placed = block;
                schedule.add(new ScheduleEntry(task, day, NumberUtil.round1(placed)));
                remaining.merge(task.getId(), -placed, Double::sum);
                usedToday.merge(task.getId(), placed, Double::sum);
                mustLeft.computeIfPresent(task.getId(), (k, v) -> Math.max(0.0, v - placed));
                capacity -= placed;
            }
        }

        LocalDate horizonEnd = start.plusDays(days);
        for (Task t : active) {
            double left = remaining.get(t.getId());
            // only warn when the deadline falls inside the planning window (or is already past)
            if (left > 0.05 && t.getDueDate().isBefore(horizonEnd)) {
                schedule.markAtRisk(t, NumberUtil.round1(left));
            }
        }
        return schedule;
    }

    /**
     * For each task, the hours that cannot be pushed to a later day without missing the
     * deadline. Computed by filling future days backwards, latest deadline first.
     */
    private Map<String, Double> mustDoToday(List<Task> active, Map<String, Double> remaining, LocalDate day) {
        Map<LocalDate, Double> futureCapacity = new HashMap<>();
        Map<String, Double> must = new HashMap<>();
        List<Task> byDeadlineDesc = active.stream()
                .filter(t -> remaining.get(t.getId()) > EPS)
                .filter(t -> !t.getDueDate().isBefore(day))
                .sorted(Comparator.comparing(Task::getDueDate).reversed())
                .collect(Collectors.toList());

        for (Task t : byDeadlineDesc) {
            double left = remaining.get(t.getId());
            for (LocalDate d = t.getDueDate(); d.isAfter(day) && left > EPS; d = d.minusDays(1)) {
                double free = futureCapacity.getOrDefault(d, dailyHours);
                double place = Math.min(Math.min(free, left), maxBlockHours);
                futureCapacity.put(d, free - place);
                left -= place;
            }
            must.put(t.getId(), left);
        }
        return must;
    }
}
