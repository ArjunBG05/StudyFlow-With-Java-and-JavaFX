package com.studyflow.model;

import com.studyflow.util.Validator;

import java.time.LocalDate;

/** A long-running deliverable, possibly done in a team. */
public class Project extends Task {

    private int teamSize;

    public Project(String id, String title, Subject subject, LocalDate dueDate,
                   double estimatedHours, Difficulty difficulty, int teamSize) {
        super(id, title, subject, dueDate, estimatedHours, difficulty);
        setTeamSize(teamSize);
    }

    public int getTeamSize() {
        return teamSize;
    }

    public final void setTeamSize(int teamSize) {
        this.teamSize = Validator.requireRange(teamSize, 1, 20, "Team size");
    }

    @Override
    public TaskType getType() {
        return TaskType.PROJECT;
    }

    /** Solo projects are slightly more pressing: nobody shares the load. */
    @Override
    public double getTypeWeight() {
        return teamSize == 1 ? 1.1 : 1.0;
    }

    @Override
    protected String extraRecord() {
        return String.valueOf(teamSize);
    }

    @Override
    public String getDetails() {
        return teamSize == 1 ? "solo project" : "team of " + teamSize;
    }
}
