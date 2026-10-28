package com.studyflow.service;

import com.studyflow.model.Task;

import java.time.LocalDate;

/** One planned block of study: immutable value object. */
public final class ScheduleEntry {

    private final Task task;
    private final LocalDate date;
    private final double hours;

    public ScheduleEntry(Task task, LocalDate date, double hours) {
        this.task = task;
        this.date = date;
        this.hours = hours;
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

    @Override
    public String toString() {
        return date + " " + hours + "h " + task.getTitle();
    }
}
