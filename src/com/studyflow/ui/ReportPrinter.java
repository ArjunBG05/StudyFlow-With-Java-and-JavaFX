package com.studyflow.ui;

import com.studyflow.model.Task;
import com.studyflow.service.OverallSummary;
import com.studyflow.service.Schedule;
import com.studyflow.service.ScheduleEntry;
import com.studyflow.service.SubjectReport;
import com.studyflow.strategy.RankedTask;
import com.studyflow.util.TextUtil;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Formats reports. Writes to any PrintStream, so the same code prints to the console or a file. */
public class ReportPrinter {

    private static final String LINE = "------------------------------------------------------------------------------";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE dd MMM");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy");

    private final PrintStream out;

    public ReportPrinter(PrintStream out) {
        this.out = out;
    }

    public void heading(String title) {
        out.println();
        out.println("== " + title + " ==");
        out.println(LINE);
    }

    public void printRanking(List<RankedTask> ranked, LocalDate today, String strategyName) {
        heading("Priority ranking");
        out.println("Strategy: " + strategyName);
        if (ranked.isEmpty()) {
            out.println("No active tasks. Add one from the menu.");
            return;
        }
        out.printf("%-3s %-6s %-9s %-11s %-10s %-24s %-17s %s%n",
                "#", "Score", "Level", "Type", "Subject", "Task", "Due", "Left");
        int rank = 1;
        for (RankedTask rt : ranked) {
            Task t = rt.getTask();
            long days = t.daysLeft(today);
            String due = t.getDueDate().format(SHORT_DATE)
                    + (days < 0 ? " (late)" : " (" + days + "d)");
            out.printf("%-3d %-6.1f %-9s %-11s %-10s %-24s %-17s %.1fh%n",
                    rank++, rt.getScore(), rt.getLevel().getLabel(), t.getType().getLabel(),
                    TextUtil.truncate(t.getSubject().getName(), 10),
                    TextUtil.truncate(t.getTitle(), 24), due, t.getRemainingHours());
        }
    }

    public void printTasks(List<Task> tasks, LocalDate today) {
        heading("All tasks");
        if (tasks.isEmpty()) {
            out.println("No tasks yet.");
            return;
        }
        out.printf("%-6s %-11s %-10s %-24s %-12s %-12s %s%n",
                "ID", "Type", "Subject", "Title", "Due", "Status", "Progress");
        for (Task t : tasks) {
            out.printf("%-6s %-11s %-10s %-24s %-12s %-12s %5.1f%%  %s%n",
                    t.getId(), t.getType().getLabel(), TextUtil.truncate(t.getSubject().getName(), 10),
                    TextUtil.truncate(t.getTitle(), 24), t.getDueDate(), t.getStatus().getLabel(),
                    t.getProgressPercent(), t.getDetails());
        }
    }

    public void printReports(List<SubjectReport> reports, OverallSummary summary) {
        heading("Subject progress");
        if (reports.isEmpty()) {
            out.println("No subjects yet.");
            return;
        }
        for (SubjectReport r : reports) {
            out.printf("%-12s %5.1f%% |%-20s| %s%n", r.getSubjectName(), r.getProgress(),
                    bar(r.getProgress()), r.getStatus().getLabel());
            out.printf("             %.1fh studied, %d active / %d done, %d overdue, prep %d%%, next due %s%n",
                    r.getHoursStudied(), r.getActiveTasks(), r.getCompletedTasks(), r.getOverdueTasks(),
                    r.getPreparation(), r.getNextDeadline().map(LocalDate::toString).orElse("-"));
        }
        out.println(LINE);
        out.printf("Overall: %d/%d tasks complete (%.0f%%), %.1fh studied in total, %.1fh in the last 7 days%n",
                summary.getCompletedTasks(), summary.getTotalTasks(), summary.getCompletionRate(),
                summary.getTotalHours(), summary.getHoursLast7Days());
        summary.getFocusSubject().ifPresent(s -> out.println("Suggested focus right now: " + s));
        if (summary.getOverdueTasks() > 0) {
            out.println("Warning: " + summary.getOverdueTasks() + " overdue task(s)!");
        }
    }

    public void printSchedule(Schedule schedule) {
        heading("Study plan (" + schedule.getDays() + " days from " + schedule.getStart() + ")");
        Map<LocalDate, List<ScheduleEntry>> byDay = schedule.getEntriesByDay();
        if (byDay.isEmpty()) {
            out.println("Nothing to schedule.");
        }
        for (Map.Entry<LocalDate, List<ScheduleEntry>> day : byDay.entrySet()) {
            out.printf("%s  (%.1fh)%n", day.getKey().format(LONG_DATE), schedule.getHoursOn(day.getKey()));
            for (ScheduleEntry e : day.getValue()) {
                out.printf("    %4.1fh  %-10s %-10s %s%n", e.getHours(),
                        TextUtil.truncate(e.getTask().getSubject().getName(), 10),
                        e.getTask().getType().getLabel(), e.getTask().getTitle());
            }
        }
        out.printf("Total planned: %.1fh%n", schedule.getTotalHours());
        if (!schedule.getAtRisk().isEmpty()) {
            out.println();
            out.println("WARNING - these cannot be finished before their deadline at your current daily hours:");
            schedule.getAtRisk().forEach((task, hrs) ->
                    out.printf("    %s (%s): %.1fh short, so add study time or move the deadline%n",
                            task.getTitle(), task.getSubject().getName(), hrs));
        }
    }

    private static String bar(double percent) {
        int filled = (int) Math.round(percent / 5.0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append(i < filled ? '#' : ' ');
        }
        return sb.toString();
    }
}
