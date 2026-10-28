package com.studyflow.model;

import com.studyflow.exception.InvalidInputException;

/** Difficulty scale 1-5. An enum with fields keeps the numbers and labels together. */
public enum Difficulty {
    VERY_EASY(1, "Very easy"),
    EASY(2, "Easy"),
    MEDIUM(3, "Medium"),
    HARD(4, "Hard"),
    VERY_HARD(5, "Very hard");

    private final int level;
    private final String label;

    Difficulty(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    /** Difficulty normalised to the range 0.2 - 1.0. */
    public double getWeight() {
        return level / 5.0;
    }

    public static Difficulty fromLevel(int level) {
        for (Difficulty d : values()) {
            if (d.level == level) {
                return d;
            }
        }
        throw new InvalidInputException("Difficulty must be between 1 and 5");
    }
}
