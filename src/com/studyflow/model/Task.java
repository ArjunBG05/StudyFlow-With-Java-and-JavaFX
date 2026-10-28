package com.studyflow.model;

import com.studyflow.exception.InvalidInputException;
import com.studyflow.util.NumberUtil;
import com.studyflow.util.TextUtil;
import com.studyflow.util.Validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Abstract base class for every kind of academic work.
 *
 * Shared state and behaviour live here; the parts that differ between an
 * Assignment, an Exam and a Project are left to subclasses (polymorphism):
 * getType(), getTypeWeight(), getReadinessGap(), extraRecord() ...
 *
 * toRecord() is a Template Method: the fixed layout is defined here and the
 * subclass supplies only its extra fields.
 */
public abstract class Task implements Identifiable, Trackable, Persistable, Comparable<Task> {

    private final String id;
    private final Subject subject;
    private String title;
    private LocalDate dueDate;
    private double estimatedHours;
    private Difficulty difficulty;
    private double hoursDone;
    private Status status = Status.PENDING;

    protected Task(String id, String title, Subject subject, LocalDate dueDate,
                   double estimatedHours, Difficulty difficulty) {
        this.id = Validator.requireText(id, "Task id");
        this.subject = Validator.requireNonNull(subject, "Subject");
        setTitle(title);
        setDueDate(dueDate);
        setEstimatedHours(estimatedHours);
        setDifficulty(difficulty);
    }

    // ------------------------------------------------------------------
    // Abstract / overridable behaviour
    // ------------------------------------------------------------------
    public abstract TaskType getType();

    /** How much this kind of task matters compared with others (roughly 0.9 - 1.4). */
    public abstract double getTypeWeight();

    /** Subclass-specific fields appended to the data record, e.g. "12|3". May be empty. */
    protected abstract String extraRecord();

    /** One-line description of the subclass-specific details, for display. */
    public abstract String getDetails();

    /** 0 (fully prepared) to 1 (not prepared). Only exams override this. */
    public double getReadinessGap() {
        return 0.0;
    }

    /** True if getReadinessGap() should influence the priority score. */
    public boolean isReadinessSensitive() {
        return false;
    }

    // ------------------------------------------------------------------
    // Getters and validated setters
    // ------------------------------------------------------------------
    @Override
    public String getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = Validator.requireText(title, "Task title");
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public final void setDueDate(LocalDate dueDate) {
        this.dueDate = Validator.requireNonNull(dueDate, "Due date");
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public final void setEstimatedHours(double hours) {
        this.estimatedHours = Validator.requirePositive(hours, "Estimated hours");
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public final void setDifficulty(Difficulty difficulty) {
        this.difficulty = Validator.requireNonNull(difficulty, "Difficulty");
    }

    public double getHoursDone() {
        return hoursDone;
    }

    public Status getStatus() {
        return status;
    }

    // ------------------------------------------------------------------
    // Behaviour shared by all tasks
    // ------------------------------------------------------------------
    public boolean isActive() {
        return status != Status.COMPLETED;
    }

    public long daysLeft(LocalDate today) {
        return ChronoUnit.DAYS.between(today, dueDate);
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && dueDate.isBefore(today);
    }

    public double getRemainingHours() {
        return isActive() ? Math.max(0.0, estimatedHours - hoursDone) : 0.0;
    }

    /** Hours counted towards progress; a completed task always counts as fully done. */
    public double getEffectiveHoursDone() {
        return isActive() ? Math.min(hoursDone, estimatedHours) : estimatedHours;
    }

    /** Records real study time. The task completes automatically once the estimate is reached. */
    public void logHours(double hours) {
        Validator.requirePositive(hours, "Hours");
        if (!isActive()) {
            throw new InvalidInputException("Task '" + title + "' is already completed");
        }
        hoursDone += hours;
        status = hoursDone >= estimatedHours ? Status.COMPLETED : Status.IN_PROGRESS;
    }

    public void markCompleted() {
        status = Status.COMPLETED;
    }

    /** Used when loading from disk to put the task back into its saved state. */
    public void restoreState(double hoursDone, Status status) {
        if (hoursDone < 0) {
            throw new InvalidInputException("Hours done cannot be negative");
        }
        this.hoursDone = hoursDone;
        this.status = Validator.requireNonNull(status, "Status");
    }

    @Override
    public double getProgressPercent() {
        if (!isActive()) {
            return 100.0;
        }
        return NumberUtil.round1(Math.min(100.0, 100.0 * hoursDone / estimatedHours));
    }

    @Override
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    // ------------------------------------------------------------------
    // Persistence (Template Method)
    // ------------------------------------------------------------------
    @Override
    public final String toRecord() {
        String base = String.join("|",
                getType().name(), id, subject.getId(), TextUtil.escape(title),
                dueDate.toString(), Double.toString(estimatedHours),
                String.valueOf(difficulty.getLevel()), Double.toString(hoursDone), status.name());
        String extra = extraRecord();
        return extra.isEmpty() ? base : base + "|" + extra;
    }

    // ------------------------------------------------------------------
    // Object methods
    // ------------------------------------------------------------------
    /** Natural ordering: earliest deadline first. */
    @Override
    public int compareTo(Task other) {
        int byDate = dueDate.compareTo(other.dueDate);
        return byDate != 0 ? byDate : title.compareToIgnoreCase(other.title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Task)) {
            return false;
        }
        return id.equals(((Task) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getType().getLabel() + "[" + id + "] " + title + " (" + subject.getName()
                + ", due " + dueDate + ")";
    }
}
