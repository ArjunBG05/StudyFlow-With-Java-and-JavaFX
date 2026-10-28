package com.studyflow.model;

import com.studyflow.util.Validator;

import java.time.LocalDate;

/** Coursework. Carries a grade weight: worth more marks means more important. */
public class Assignment extends Task {

    private int gradeWeight;    // percent of the final grade, 0-100

    public Assignment(String id, String title, Subject subject, LocalDate dueDate,
                      double estimatedHours, Difficulty difficulty, int gradeWeight) {
        super(id, title, subject, dueDate, estimatedHours, difficulty);
        setGradeWeight(gradeWeight);
    }

    public int getGradeWeight() {
        return gradeWeight;
    }

    public final void setGradeWeight(int gradeWeight) {
        this.gradeWeight = Validator.requireRange(gradeWeight, 0, 100, "Grade weight");
    }

    @Override
    public TaskType getType() {
        return TaskType.ASSIGNMENT;
    }

    /** 0.90 for a worthless assignment up to 1.30 for one worth 100% of the grade. */
    @Override
    public double getTypeWeight() {
        return 0.9 + gradeWeight / 250.0;
    }

    @Override
    protected String extraRecord() {
        return String.valueOf(gradeWeight);
    }

    @Override
    public String getDetails() {
        return "worth " + gradeWeight + "% of grade";
    }
}
