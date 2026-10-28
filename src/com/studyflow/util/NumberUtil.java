package com.studyflow.util;

public final class NumberUtil {

    private NumberUtil() { }

    /** Rounds to one decimal place. */
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
