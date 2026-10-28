package com.studyflow.service;

import com.studyflow.model.StudySession;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.strategy.PriorityStrategy;
import com.studyflow.util.NumberUtil;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Monitors subject progress. Uses streams and lambdas to aggregate the data. */
public class ProgressTracker {

    private final PriorityStrategy strategy;

    public ProgressTracker(PriorityStrategy strategy) {
        this.strategy = strategy;
    }

    public SubjectReport report(Subject subject, LocalDate today) {
        List<Task> tasks = subject.getTasks();
        List<Task> active = tasks.stream().filter(Task::isActive).collect(Collectors.toList());

        double avgPriority = active.stream()
                .mapToDouble(t -> strategy.calculate(t, today)).average().orElse(0.0);
        LocalDate next = active.stream().map(Task::getDueDate).min(Comparator.naturalOrder()).orElse(null);
        long overdue = subject.getOverdueTaskCount(today);

        return new SubjectReport(subject.getName(), subject.getProgressPercent(), subject.getHoursStudied(),
                active.size(), tasks.size() - active.size(), overdue, next,
                NumberUtil.round1(avgPriority), subject.getPreparationLevel(),
                classify(tasks.isEmpty(), active.isEmpty(), overdue, avgPriority));
    }

    public List<SubjectReport> reportAll(Collection<Subject> subjects, LocalDate today) {
        return subjects.stream().map(s -> report(s, today)).collect(Collectors.toList());
    }

    public OverallSummary summarise(Collection<Subject> subjects, Collection<StudySession> sessions,
                                    LocalDate today) {
        List<Task> all = subjects.stream().flatMap(s -> s.getTasks().stream()).collect(Collectors.toList());
        int completed = (int) all.stream().filter(Task::isCompleted).count();
        int overdue = (int) all.stream().filter(t -> t.isOverdue(today)).count();
        double hours = all.stream().mapToDouble(Task::getHoursDone).sum();
        LocalDate weekAgo = today.minusDays(6);
        double lastWeek = sessions.stream()
                .filter(StudySession::isCompleted)
                .filter(s -> !s.getDate().isBefore(weekAgo) && !s.getDate().isAfter(today))
                .mapToDouble(StudySession::getHours).sum();

        String focus = subjects.stream()
                .filter(s -> s.getActiveTaskCount() > 0)
                .max(Comparator.comparingDouble(s -> report(s, today).getAveragePriority()))
                .map(Subject::getName).orElse(null);

        return new OverallSummary(all.size(), completed, overdue, hours, lastWeek, focus);
    }

    private HealthStatus classify(boolean noTasks, boolean noneActive, long overdue, double avgPriority) {
        if (noTasks) {
            return HealthStatus.NO_TASKS;
        }
        if (noneActive) {
            return HealthStatus.ON_TRACK;
        }
        if (overdue > 0 || avgPriority >= 60) {
            return HealthStatus.AT_RISK;
        }
        return avgPriority >= 35 ? HealthStatus.NEEDS_ATTENTION : HealthStatus.ON_TRACK;
    }
}
