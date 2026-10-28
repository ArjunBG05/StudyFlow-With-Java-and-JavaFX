package com.studyflow.model;

import com.studyflow.exception.InvalidInputException;
import com.studyflow.util.Validator;

import java.time.LocalDate;

/**
 * An exam. Exams weigh more than other tasks and are the only tasks whose
 * priority depends on how prepared the student already is.
 */
public class Exam extends Task {

    private final int totalTopics;
    private int revisedTopics;

    public Exam(String id, String title, Subject subject, LocalDate dueDate,
                double estimatedHours, Difficulty difficulty, int totalTopics, int revisedTopics) {
        super(id, title, subject, dueDate, estimatedHours, difficulty);
        this.totalTopics = Validator.requireRange(totalTopics, 1, 500, "Total topics");
        this.revisedTopics = Validator.requireRange(revisedTopics, 0, totalTopics, "Revised topics");
    }

    public int getTotalTopics() {
        return totalTopics;
    }

    public int getRevisedTopics() {
        return revisedTopics;
    }

    public void reviseTopics(int count) {
        if (count <= 0) {
            throw new InvalidInputException("Topic count must be positive");
        }
        revisedTopics = Math.min(totalTopics, revisedTopics + count);
    }

    @Override
    public TaskType getType() {
        return TaskType.EXAM;
    }

    @Override
    public double getTypeWeight() {
        return 1.4;
    }

    /** Blends the subject's self-rated preparation (60%) with syllabus coverage (40%). */
    @Override
    public double getReadinessGap() {
        double prep = getSubject().getPreparationLevel() / 100.0;
        double coverage = (double) revisedTopics / totalTopics;
        return 1.0 - (0.6 * prep + 0.4 * coverage);
    }

    @Override
    public boolean isReadinessSensitive() {
        return true;
    }

    @Override
    protected String extraRecord() {
        return totalTopics + "|" + revisedTopics;
    }

    @Override
    public String getDetails() {
        return revisedTopics + "/" + totalTopics + " topics revised";
    }
}
