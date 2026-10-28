package com.studyflow.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/** Validated console input. Every method keeps asking until the answer is acceptable. */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String text(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("  Please enter some text.");
        }
    }

    public int integer(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " [" + min + "-" + max + "]: ");
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("  Enter a whole number from " + min + " to " + max + ".");
        }
    }

    public double number(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt + " [" + min + "-" + max + "]: ");
            try {
                double v = Double.parseDouble(scanner.nextLine().trim());
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("  Enter a number from " + min + " to " + max + ".");
        }
    }

    /** Accepts yyyy-MM-dd, "today", "tomorrow" or "+N" (N days from today). */
    public LocalDate date(String prompt, LocalDate today) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd, +N days, today, tomorrow): ");
            String s = scanner.nextLine().trim().toLowerCase();
            try {
                if (s.equals("today")) {
                    return today;
                }
                if (s.equals("tomorrow")) {
                    return today.plusDays(1);
                }
                if (s.startsWith("+")) {
                    return today.plusDays(Long.parseLong(s.substring(1)));
                }
                return LocalDate.parse(s);
            } catch (NumberFormatException | DateTimeParseException e) {
                System.out.println("  Could not read that date.");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String s = scanner.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
        }
    }

    /** Shows a numbered list and returns the chosen element. */
    public <T> T choose(String prompt, List<T> options, Function<T, String> label) {
        System.out.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d) %s%n", i + 1, label.apply(options.get(i)));
        }
        return options.get(integer("Choice", 1, options.size()) - 1);
    }
}
