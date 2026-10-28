package com.studyflow.ui;

import com.studyflow.exception.EntityNotFoundException;
import com.studyflow.exception.InvalidInputException;
import com.studyflow.exception.StorageException;
import com.studyflow.exception.StudyFlowException;
import com.studyflow.model.Difficulty;
import com.studyflow.model.StudySession;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.model.TaskType;
import com.studyflow.service.StudyFlowService;
import com.studyflow.storage.DataStore;
import com.studyflow.strategy.PriorityEngine;
import com.studyflow.strategy.PriorityStrategy;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Menu-driven console front end. It contains no business rules, only input and output. */
public class ConsoleApp {

    private final StudyFlowService service;
    private final DataStore store;
    private final ConsoleInput in;
    private final ReportPrinter printer = new ReportPrinter(System.out);

    public ConsoleApp(StudyFlowService service, DataStore store) {
        this.service = service;
        this.store = store;
        this.in = new ConsoleInput(new Scanner(System.in));
        service.addListener(new ConsoleNotifier(System.out));
    }

    public void run() {
        System.out.println("=====================================");
        System.out.println("   StudyFlow - Student Productivity  ");
        System.out.println("=====================================");
        loadSavedData();
        try {
            boolean running = true;
            while (running) {
                showMenu();
                int choice = in.integer("Select", 0, 14);
                try {
                    running = handle(choice);
                } catch (InvalidInputException | StudyFlowException e) {
                    System.out.println("  Error: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException endOfInput) {
            System.out.println("\nInput closed.");
        }
        saveData();
        System.out.println("Goodbye, and good luck with your studies!");
    }

    private void showMenu() {
        System.out.println();
        System.out.println("--- MENU (today: " + service.today() + ") ---");
        System.out.println(" 1) Add subject             8) Deadline alerts");
        System.out.println(" 2) Add task                9) Change priority strategy");
        System.out.println(" 3) Priority ranking       10) Update subject preparation");
        System.out.println(" 4) Log study session      11) Change a task's deadline");
        System.out.println(" 5) Mark task complete     12) Delete task or subject");
        System.out.println(" 6) Progress report        13) Export plan & report to file");
        System.out.println(" 7) Generate study plan    14) Load sample data");
        System.out.println(" 0) Save and exit");
    }

    private boolean handle(int choice) throws StudyFlowException {
        switch (choice) {
            case 1:
                addSubject();
                break;
            case 2:
                addTask();
                break;
            case 3:
                printer.printRanking(service.getRankedTasks(), service.today(), service.getStrategy().getName());
                break;
            case 4:
                logSession();
                break;
            case 5:
                completeTask();
                break;
            case 6:
                printer.printReports(service.getSubjectReports(), service.getSummary());
                break;
            case 7:
                generatePlan();
                break;
            case 8:
                deadlineAlerts();
                break;
            case 9:
                changeStrategy();
                break;
            case 10:
                updatePreparation();
                break;
            case 11:
                changeDeadline();
                break;
            case 12:
                deleteItem();
                break;
            case 13:
                exportReport();
                break;
            case 14:
                loadSample();
                break;
            case 0:
                return false;
            default:
                break;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------------
    private void addSubject() {
        String name = in.text("Subject name");
        int difficulty = in.integer("Difficulty (1 easy - 5 very hard)", 1, 5);
        int prep = in.integer("How prepared do you feel (%)", 0, 100);
        Subject s = service.addSubject(name, difficulty, prep);
        System.out.println("  Added subject " + s.getId() + ": " + s);
    }

    private void addTask() throws StudyFlowException {
        Subject subject = chooseSubject();
        TaskType type = in.choose("Task type:", Arrays.asList(TaskType.values()), TaskType::getLabel);
        String title = in.text("Title");
        LocalDate due = in.date("Due date", service.today());
        double hours = in.number("Estimated hours of work", 0.5, 300);
        int diff = in.integer("Difficulty (0 = same as subject)", 0, 5);
        Difficulty difficulty = diff == 0 ? null : Difficulty.fromLevel(diff);

        Task created;
        switch (type) {
            case ASSIGNMENT:
                created = service.addAssignment(subject.getId(), title, due, hours, difficulty,
                        in.integer("Weight in final grade (%)", 0, 100));
                break;
            case EXAM:
                int total = in.integer("Number of syllabus topics", 1, 500);
                int revised = in.integer("Topics already revised", 0, total);
                created = service.addExam(subject.getId(), title, due, hours, difficulty, total, revised);
                break;
            default:
                created = service.addProject(subject.getId(), title, due, hours, difficulty,
                        in.integer("Team size (1 = solo)", 1, 20));
                break;
        }
        System.out.println("  Added " + created);
    }

    private void logSession() throws StudyFlowException {
        Task task = chooseActiveTask("Which task did you study for?");
        double hours = in.number("Hours studied", 0.25, 16);
        StudySession s = service.logSession(task.getId(), hours, service.today());
        System.out.printf("  Logged %.2fh. '%s' is now %.1f%% done.%n",
                s.getHours(), task.getTitle(), task.getProgressPercent());
    }

    private void completeTask() throws StudyFlowException {
        Task task = chooseActiveTask("Which task is finished?");
        service.completeTask(task.getId());
    }

    private void generatePlan() {
        int days = in.integer("Plan how many days", 1, 30);
        double hours = in.number("Study hours available per day", 0.5, 16);
        service.getScheduler().setDailyHours(hours);
        printer.printSchedule(service.generateSchedule(days));
    }

    private void deadlineAlerts() {
        int within = in.integer("Alert for tasks due within how many days", 0, 60);
        List<Task> due = service.checkDeadlines(within);
        if (due.isEmpty()) {
            System.out.println("  Nothing due in the next " + within + " days. Nice!");
        }
    }

    private void changeStrategy() {
        List<PriorityStrategy> options = PriorityEngine.availableStrategies();
        PriorityStrategy chosen = in.choose("Priority strategy (current: " + service.getStrategy().getName() + "):",
                options, s -> s.getName() + " - " + s.getDescription());
        service.setStrategy(chosen);
        System.out.println("  Now using: " + chosen.getName());
    }

    private void updatePreparation() throws StudyFlowException {
        Subject s = chooseSubject();
        service.updatePreparation(s.getId(), in.integer("New preparation level (%)", 0, 100));
        System.out.println("  Updated " + s);
    }

    private void changeDeadline() throws StudyFlowException {
        Task task = chooseActiveTask("Which task's deadline changes?");
        service.rescheduleTask(task.getId(), in.date("New due date", service.today()));
        System.out.println("  Updated " + task);
    }

    private void deleteItem() throws StudyFlowException {
        int what = in.integer("Delete: 1) a task  2) a whole subject", 1, 2);
        if (what == 1) {
            List<Task> all = service.getTasks();
            if (all.isEmpty()) {
                System.out.println("  No tasks to delete.");
                return;
            }
            Task t = in.choose("Which task?", all, Task::toString);
            if (in.confirm("Delete '" + t.getTitle() + "'")) {
                service.removeTask(t.getId());
                System.out.println("  Deleted.");
            }
        } else {
            Subject s = chooseSubject();
            if (in.confirm("Delete " + s.getName() + " and all its tasks")) {
                service.removeSubject(s.getId());
                System.out.println("  Deleted.");
            }
        }
    }

    private void exportReport() throws StudyFlowException {
        String file = "studyflow-report.txt";
        try (PrintStream ps = new PrintStream(Files.newOutputStream(Paths.get(file)), true,
                StandardCharsets.UTF_8.name())) {
            ReportPrinter p = new ReportPrinter(ps);
            p.printRanking(service.getRankedTasks(), service.today(), service.getStrategy().getName());
            p.printReports(service.getSubjectReports(), service.getSummary());
            p.printSchedule(service.generateSchedule(7));
        } catch (IOException e) {
            throw new StorageException("Could not write " + file, e);
        }
        System.out.println("  Report written to " + file);
    }

    private void loadSample() throws StudyFlowException {
        if (!service.isEmpty() && !in.confirm("This adds sample data to your existing data. Continue")) {
            return;
        }
        try {
            DemoData.populate(service);
            System.out.println("  Sample data loaded.");
        } catch (InvalidInputException e) {
            throw new InvalidInputException("Sample data clashes with existing subjects (" + e.getMessage() + ")");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Subject chooseSubject() throws EntityNotFoundException {
        List<Subject> subjects = service.getSubjects();
        if (subjects.isEmpty()) {
            throw new EntityNotFoundException("Subject", "(none yet, add one first)");
        }
        return in.choose("Choose a subject:", subjects, Subject::toString);
    }

    private Task chooseActiveTask(String prompt) throws EntityNotFoundException {
        List<Task> active = service.getTasksByDeadline();
        if (active.isEmpty()) {
            throw new EntityNotFoundException("Active task", "(none left)");
        }
        return in.choose(prompt, active, t -> t + " - " + String.format("%.1f", t.getRemainingHours()) + "h left");
    }

    private void loadSavedData() {
        try {
            if (store.exists()) {
                service.importData(store.load());
                System.out.println("Loaded " + service.getSubjects().size() + " subject(s) and "
                        + service.getTasks().size() + " task(s) from the data file.");
            } else {
                System.out.println("No saved data yet. Tip: choose option 14 to load sample data.");
            }
        } catch (StorageException e) {
            System.out.println("Warning: " + e.getMessage() + "\nStarting with an empty workspace.");
        }
    }

    private void saveData() {
        try {
            store.save(service.exportData());
            System.out.println("Data saved.");
        } catch (StorageException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }
}
