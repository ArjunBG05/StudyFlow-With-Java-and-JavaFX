package com.studyflow.model;

import com.studyflow.util.NumberUtil;
import com.studyflow.util.TextUtil;
import com.studyflow.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A course the student is studying. A Subject owns its tasks (aggregation)
 * and knows how to measure its own progress.
 */
public class Subject implements Identifiable, Trackable, Persistable {

    private final String id;
    private String name;
    private Difficulty difficulty;
    private int preparationLevel;           // self-rated readiness, 0-100
    private final List<Task> tasks = new ArrayList<>();

    public Subject(String id, String name, Difficulty difficulty, int preparationLevel) {
        this.id = Validator.requireText(id, "Subject id");
        setName(name);
        setDifficulty(difficulty);
        setPreparationLevel(preparationLevel);
    }

    // ---- getters / validated setters (encapsulation) ----
    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = Validator.requireText(name, "Subject name");
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public final void setDifficulty(Difficulty difficulty) {
        this.difficulty = Validator.requireNonNull(difficulty, "Difficulty");
    }

    public int getPreparationLevel() {
        return preparationLevel;
    }

    public final void setPreparationLevel(int level) {
        this.preparationLevel = Validator.requireRange(level, 0, 100, "Preparation level");
    }

    // ---- task management ----
    public void addTask(Task task) {
        Validator.requireNonNull(task, "Task");
        if (!tasks.contains(task)) {
            tasks.add(task);
        }
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    /** Read-only view so outside code cannot bypass addTask/removeTask. */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    // ---- progress ----
    /** Percentage of all estimated work that has been completed, weighted by task size. */
    @Override
    public double getProgressPercent() {
        double total = tasks.stream().mapToDouble(Task::getEstimatedHours).sum();
        if (total == 0) {
            return 0.0;
        }
        double done = tasks.stream().mapToDouble(Task::getEffectiveHoursDone).sum();
        return NumberUtil.round1(100.0 * done / total);
    }

    @Override
    public boolean isCompleted() {
        return !tasks.isEmpty() && tasks.stream().allMatch(Task::isCompleted);
    }

    public double getHoursStudied() {
        return tasks.stream().mapToDouble(Task::getHoursDone).sum();
    }

    public long getActiveTaskCount() {
        return tasks.stream().filter(Task::isActive).count();
    }

    public long getOverdueTaskCount(LocalDate today) {
        return tasks.stream().filter(t -> t.isOverdue(today)).count();
    }

    // ---- persistence ----
    @Override
    public String toRecord() {
        return String.join("|", "SUBJECT", id, TextUtil.escape(name),
                String.valueOf(difficulty.getLevel()), String.valueOf(preparationLevel));
    }

    public static Subject fromFields(String[] f) {
        return new Subject(f[1], TextUtil.unescape(f[2]),
                Difficulty.fromLevel(Integer.parseInt(f[3])), Integer.parseInt(f[4]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subject)) {
            return false;
        }
        return id.equals(((Subject) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (difficulty " + difficulty.getLevel() + ", prep " + preparationLevel + "%)";
    }
}
