package com.studyflow.model;

import com.studyflow.exception.InvalidInputException;
import com.studyflow.util.Validator;

import java.time.LocalDate;
import java.util.Map;

/** A block of study time. Logged sessions are completed; planned ones are not. */
public class StudySession implements Identifiable, Persistable {

    private final String id;
    private final Task task;
    private final LocalDate date;
    private final double hours;
    private final boolean completed;

    public StudySession(String id, Task task, LocalDate date, double hours, boolean completed) {
        this.id = Validator.requireText(id, "Session id");
        this.task = Validator.requireNonNull(task, "Task");
        this.date = Validator.requireNonNull(date, "Date");
        this.hours = Validator.requirePositive(hours, "Hours");
        this.completed = completed;
    }

    @Override
    public String getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getHours() {
        return hours;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String toRecord() {
        return String.join("|", "SESSION", id, task.getId(), date.toString(),
                Double.toString(hours), String.valueOf(completed));
    }

    public static StudySession fromFields(String[] f, Map<String, Task> tasksById) {
        Task t = tasksById.get(f[2]);
        if (t == null) {
            throw new InvalidInputException("Session refers to unknown task " + f[2]);
        }
        return new StudySession(f[1], t, LocalDate.parse(f[3]),
                Double.parseDouble(f[4]), Boolean.parseBoolean(f[5]));
    }

    @Override
    public String toString() {
        return date + " " + hours + "h on " + task.getTitle();
    }
}
