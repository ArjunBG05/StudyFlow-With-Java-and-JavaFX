package com.studyflow.ui;

import com.studyflow.model.Task;
import com.studyflow.service.StudyEventListener;

import java.io.PrintStream;

/** Concrete Observer that prints alerts to the console. */
public class ConsoleNotifier implements StudyEventListener {

    private final PrintStream out;

    public ConsoleNotifier(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onTaskCompleted(Task task) {
        out.println("  [DONE] Well done! '" + task.getTitle() + "' (" + task.getSubject().getName() + ") is complete.");
    }

    @Override
    public void onDeadlineApproaching(Task task, long daysLeft) {
        String when = daysLeft == 0 ? "TODAY" : "in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s");
        out.println("  [ALERT] " + task.getType().getLabel() + " '" + task.getTitle() + "' ("
                + task.getSubject().getName() + ") is due " + when
                + ", " + String.format("%.1f", task.getRemainingHours()) + "h still to do.");
    }

    @Override
    public void onTaskOverdue(Task task, long daysOverdue) {
        out.println("  [OVERDUE] '" + task.getTitle() + "' (" + task.getSubject().getName()
                + ") was due " + daysOverdue + " day" + (daysOverdue == 1 ? "" : "s") + " ago!");
    }
}
