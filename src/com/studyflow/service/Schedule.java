package com.studyflow.service;

import com.studyflow.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The result of planning: study blocks plus a list of tasks that do not fit before their deadline. */
public class Schedule {

    private final LocalDate start;
    private final int days;
    private final List<ScheduleEntry> entries = new ArrayList<>();
    private final Map<Task, Double> atRisk = new LinkedHashMap<>();

    public Schedule(LocalDate start, int days) {
        this.start = start;
        this.days = days;
    }

    public void add(ScheduleEntry entry) {
        entries.add(entry);
    }

    public void markAtRisk(Task task, double hoursShort) {
        atRisk.put(task, hoursShort);
    }

    public LocalDate getStart() {
        return start;
    }

    public int getDays() {
        return days;
    }

    public List<ScheduleEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Hours that could not be placed before the deadline, per task. */
    public Map<Task, Double> getAtRisk() {
        return Collections.unmodifiableMap(atRisk);
    }

    public Map<LocalDate, List<ScheduleEntry>> getEntriesByDay() {
        return entries.stream().collect(
                Collectors.groupingBy(ScheduleEntry::getDate, TreeMap::new, Collectors.toList()));
    }

    public double getTotalHours() {
        return entries.stream().mapToDouble(ScheduleEntry::getHours).sum();
    }

    public double getHoursOn(LocalDate date) {
        return entries.stream().filter(e -> e.getDate().equals(date))
                .mapToDouble(ScheduleEntry::getHours).sum();
    }

    public double getHoursFor(Task task) {
        return entries.stream().filter(e -> e.getTask().equals(task))
                .mapToDouble(ScheduleEntry::getHours).sum();
    }
}
