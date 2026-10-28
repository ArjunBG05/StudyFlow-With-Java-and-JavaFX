package com.studyflow.service;

import com.studyflow.exception.EntityNotFoundException;
import com.studyflow.exception.InvalidInputException;
import com.studyflow.factory.TaskFactory;
import com.studyflow.model.Difficulty;
import com.studyflow.model.StudySession;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.storage.StudyData;
import com.studyflow.strategy.PriorityEngine;
import com.studyflow.strategy.PriorityStrategy;
import com.studyflow.strategy.RankedTask;
import com.studyflow.strategy.WeightedPriorityStrategy;
import com.studyflow.util.IdGenerator;
import com.studyflow.util.Validator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade: the one class the user interface talks to. It coordinates the
 * repositories, the priority engine, the scheduler and the progress tracker,
 * and publishes events to registered listeners (Observer).
 *
 * The Clock is injected so tests can fix "today" (dependency injection).
 */
public class StudyFlowService {

    private final Clock clock;
    private final Repository<Subject> subjects = new Repository<>("Subject");
    private final Repository<Task> tasks = new Repository<>("Task");
    private final Repository<StudySession> sessions = new Repository<>("Session");
    private final PriorityEngine engine = new PriorityEngine(new WeightedPriorityStrategy());
    private final StudyScheduler scheduler = new StudyScheduler(4.0, 2.0);
    private final List<StudyEventListener> listeners = new ArrayList<>();

    public StudyFlowService() {
        this(Clock.systemDefaultZone());
    }

    public StudyFlowService(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ------------------------------------------------------------------
    // Subjects
    // ------------------------------------------------------------------
    public Subject addSubject(String name, int difficultyLevel, int preparation) {
        String clean = Validator.requireText(name, "Subject name");
        boolean duplicate = subjects.findWhere(s -> s.getName().equalsIgnoreCase(clean)).size() > 0;
        if (duplicate) {
            throw new InvalidInputException("Subject already exists: " + clean);
        }
        Subject subject = new Subject(IdGenerator.next("S"), clean,
                Difficulty.fromLevel(difficultyLevel), preparation);
        subjects.add(subject);
        return subject;
    }

    /** Finds a subject by id or by name (case-insensitive). */
    public Subject findSubject(String key) throws EntityNotFoundException {
        return subjects.findWhere(s -> s.getId().equalsIgnoreCase(key) || s.getName().equalsIgnoreCase(key))
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Subject", key));
    }

    public void updatePreparation(String subjectKey, int level) throws EntityNotFoundException {
        findSubject(subjectKey).setPreparationLevel(level);
    }

    public void removeSubject(String subjectKey) throws EntityNotFoundException {
        Subject s = findSubject(subjectKey);
        for (Task t : new ArrayList<>(s.getTasks())) {
            removeTask(t.getId());
        }
        subjects.remove(s.getId());
    }

    public List<Subject> getSubjects() {
        return subjects.findAll();
    }

    // ------------------------------------------------------------------
    // Tasks
    // ------------------------------------------------------------------
    public Task addAssignment(String subjectKey, String title, LocalDate due, double hours,
                              Difficulty difficulty, int gradeWeight) throws EntityNotFoundException {
        Subject s = findSubject(subjectKey);
        return register(TaskFactory.createAssignment(s, title, due, hours, difficulty, gradeWeight));
    }

    public Task addExam(String subjectKey, String title, LocalDate due, double hours,
                        Difficulty difficulty, int totalTopics, int revisedTopics) throws EntityNotFoundException {
        Subject s = findSubject(subjectKey);
        return register(TaskFactory.createExam(s, title, due, hours, difficulty, totalTopics, revisedTopics));
    }

    public Task addProject(String subjectKey, String title, LocalDate due, double hours,
                           Difficulty difficulty, int teamSize) throws EntityNotFoundException {
        Subject s = findSubject(subjectKey);
        return register(TaskFactory.createProject(s, title, due, hours, difficulty, teamSize));
    }

    private Task register(Task task) {
        tasks.add(task);
        task.getSubject().addTask(task);
        return task;
    }

    public Task findTask(String id) throws EntityNotFoundException {
        return tasks.getById(id.toUpperCase());
    }

    public List<Task> getTasks() {
        return tasks.findAll();
    }

    public List<Task> getActiveTasks() {
        return tasks.findWhere(Task::isActive);
    }

    public void removeTask(String taskId) throws EntityNotFoundException {
        Task t = findTask(taskId);
        tasks.remove(t.getId());
        t.getSubject().removeTask(t);
        sessions.removeIf(s -> s.getTask().equals(t));
    }

    public void rescheduleTask(String taskId, LocalDate newDue) throws EntityNotFoundException {
        findTask(taskId).setDueDate(newDue);
    }

    public void completeTask(String taskId) throws EntityNotFoundException {
        Task t = findTask(taskId);
        if (t.isCompleted()) {
            throw new InvalidInputException("Task is already completed");
        }
        t.markCompleted();
        fireCompleted(t);
    }

    // ------------------------------------------------------------------
    // Study sessions
    // ------------------------------------------------------------------
    public StudySession logSession(String taskId, double hours, LocalDate date) throws EntityNotFoundException {
        Task t = findTask(taskId);
        t.logHours(hours);                                   // validates and updates status
        StudySession session = new StudySession(IdGenerator.next("L"), t, date, hours, true);
        sessions.add(session);
        if (t.isCompleted()) {
            fireCompleted(t);
        }
        return session;
    }

    public List<StudySession> getSessions() {
        return sessions.findAll();
    }

    // ------------------------------------------------------------------
    // Priorities, planning, reporting
    // ------------------------------------------------------------------
    public List<RankedTask> getRankedTasks() {
        return engine.rank(tasks.findAll(), today());
    }

    public PriorityStrategy getStrategy() {
        return engine.getStrategy();
    }

    public void setStrategy(PriorityStrategy strategy) {
        engine.setStrategy(strategy);
    }

    public StudyScheduler getScheduler() {
        return scheduler;
    }

    public Schedule generateSchedule(int days) {
        return scheduler.build(tasks.findAll(), today(), days, engine.getStrategy());
    }

    public List<SubjectReport> getSubjectReports() {
        return new ProgressTracker(engine.getStrategy()).reportAll(subjects.findAll(), today());
    }

    public OverallSummary getSummary() {
        return new ProgressTracker(engine.getStrategy()).summarise(subjects.findAll(), sessions.findAll(), today());
    }

    // ------------------------------------------------------------------
    // Observer support
    // ------------------------------------------------------------------
    public void addListener(StudyEventListener listener) {
        listeners.add(Validator.requireNonNull(listener, "Listener"));
    }

    /**
     * Fires events for overdue tasks and tasks due within the given number of days.
     * @return the affected tasks, most urgent first
     */
    public List<Task> checkDeadlines(int withinDays) {
        LocalDate today = today();
        List<Task> due = tasks.findWhere(Task::isActive).stream()
                .filter(t -> t.daysLeft(today) <= withinDays)
                .sorted()
                .collect(Collectors.toList());
        for (Task t : due) {
            long days = t.daysLeft(today);
            for (StudyEventListener l : listeners) {
                if (days < 0) {
                    l.onTaskOverdue(t, -days);
                } else {
                    l.onDeadlineApproaching(t, days);
                }
            }
        }
        return due;
    }

    private void fireCompleted(Task t) {
        listeners.forEach(l -> l.onTaskCompleted(t));
    }

    // ------------------------------------------------------------------
    // Persistence support
    // ------------------------------------------------------------------
    public StudyData exportData() {
        StudyData data = new StudyData();
        data.getSubjects().addAll(subjects.findAll());
        data.getTasks().addAll(tasks.findAll());
        data.getSessions().addAll(sessions.findAll());
        return data;
    }

    /** Replaces everything currently in memory with the loaded data. */
    public void importData(StudyData data) {
        subjects.clear();
        tasks.clear();
        sessions.clear();
        data.getSubjects().forEach(subjects::add);
        data.getTasks().forEach(tasks::add);
        data.getSessions().forEach(sessions::add);
    }

    public boolean isEmpty() {
        return subjects.size() == 0;
    }

    /** Tasks sorted by their natural order (earliest deadline first). */
    public List<Task> getTasksByDeadline() {
        return tasks.findWhere(Task::isActive).stream().sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
