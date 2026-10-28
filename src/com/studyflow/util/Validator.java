package com.studyflow.util;

import com.studyflow.exception.InvalidInputException;

/** Central place for argument validation so the rules are written once. */
public final class Validator {

    private Validator() { }

    public static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field + " must not be empty");
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new InvalidInputException(field + " must not be null");
        }
        return value;
    }

    public static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    public static double requirePositive(double value, String field) {
        if (Double.isNaN(value) || value <= 0) {
            throw new InvalidInputException(field + " must be greater than 0");
        }
        return value;
    }
}
