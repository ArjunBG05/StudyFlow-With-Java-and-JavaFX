package com.studyflow;

import com.studyflow.exception.EntityNotFoundException;
import com.studyflow.exception.InvalidInputException;
import com.studyflow.exception.StorageException;
import com.studyflow.model.Assignment;
import com.studyflow.model.Difficulty;
import com.studyflow.model.Exam;
import com.studyflow.model.PriorityLevel;
import com.studyflow.model.Status;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.model.TaskType;
import com.studyflow.service.HealthStatus;
import com.studyflow.service.Schedule;
import com.studyflow.service.ScheduleEntry;
import com.studyflow.service.StudyEventListener;
import com.studyflow.service.StudyFlowService;
import com.studyflow.storage.FileDataStore;
import com.studyflow.strategy.DeadlineFirstStrategy;
import com.studyflow.strategy.HardestFirstStrategy;
import com.studyflow.strategy.RankedTask;
import com.studyflow.strategy.WeightedPriorityStrategy;
import com.studyflow.ui.DemoData;
import com.studyflow.util.IdGenerator;
import com.studyflow.util.TextUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dependency-free test runner (no JUnit needed).
 * Run with: java -cp out com.studyflow.TestRunner      (exit code 1 if any test fails)
 */
public class TestRunner {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private static int passed = 0;
    private static int failed = 0;

    private interface Body {
        void run() throws Exception;
    }

    // ------------------------------------------------------------------ helpers
    private static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + "  ->  " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void eq(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected " + expected + " but was " + actual + ")");
        }
    }

    private static void near(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.05) {
            throw new AssertionError(message + " (expected " + expected + " but was " + actual + ")");
        }
    }

    private static void expect(Class<? extends Throwable> type, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but nothing was thrown");
    }

    private static StudyFlowService fresh() {
        IdGenerator.reset();
        ZoneId zone = ZoneId.systemDefault();
        return new StudyFlowService(Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone));
    }

    // ------------------------------------------------------------------ main
    public static void main(String[] args) {
        System.out.println("StudyFlow tests");
        System.out.println("---------------");

        // ---- validation & encapsulation
        test("blank subject name is rejected", () ->
                expect(InvalidInputException.class, () -> fresh().addSubject("   ", 3, 50)));
        test("difficulty must be 1-5", () ->
                expect(InvalidInputException.class, () -> fresh().addSubject("X", 9, 50)));
        test("preparation must be 0-100", () ->
                expect(InvalidInputException.class, () -> fresh().addSubject("X", 3, 101)));
        test("duplicate subject names are rejected (case-insensitive)", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            expect(InvalidInputException.class, () -> s.addSubject("MATH", 2, 10));
        });
        test("task hours must be positive", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            expect(InvalidInputException.class,
                    () -> s.addAssignment("Math", "HW", TODAY.plusDays(2), 0, null, 10));
        });
        test("unknown subject raises EntityNotFoundException", () ->
                expect(EntityNotFoundException.class,
                        () -> fresh().addAssignment("Nope", "HW", TODAY.plusDays(2), 1, null, 10)));
        test("subject's task list is read-only from outside", () -> {
            StudyFlowService s = fresh();
            Subject m = s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(2), 1, null, 10);
            expect(UnsupportedOperationException.class, () -> m.getTasks().add(t));
        });

        // ---- inheritance & polymorphism
        test("task types behave polymorphically", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task a = s.addAssignment("Math", "HW", TODAY.plusDays(5), 4, null, 10);
            Task e = s.addExam("Math", "Final", TODAY.plusDays(5), 4, null, 10, 2);
            Task p = s.addProject("Math", "Proj", TODAY.plusDays(5), 4, null, 1);
            eq(TaskType.ASSIGNMENT, a.getType(), "assignment type");
            eq(TaskType.EXAM, e.getType(), "exam type");
            eq(TaskType.PROJECT, p.getType(), "project type");
            check(e.getTypeWeight() > a.getTypeWeight(), "exam should weigh more than assignment");
            check(p.getTypeWeight() > a.getTypeWeight(), "solo project should weigh more than a light assignment");
            check(!a.getDetails().isEmpty() && !e.getDetails().isEmpty() && !p.getDetails().isEmpty(), "details");
            check(a instanceof Assignment && e instanceof Exam, "runtime types");
        });
        test("task inherits subject difficulty when none is given", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 5, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(5), 4, null, 10);
            eq(Difficulty.VERY_HARD, t.getDifficulty(), "inherited difficulty");
        });
        test("higher grade weight raises assignment weight", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task low = s.addAssignment("Math", "A", TODAY.plusDays(5), 4, null, 5);
            Task high = s.addAssignment("Math", "B", TODAY.plusDays(5), 4, null, 80);
            check(high.getTypeWeight() > low.getTypeWeight(), "weight should grow with grade share");
        });
        test("exam readiness gap shrinks as preparation grows", () -> {
            StudyFlowService s = fresh();
            Subject weak = s.addSubject("Weak", 3, 10);
            Subject strong = s.addSubject("Strong", 3, 95);
            Task e1 = s.addExam("Weak", "E1", TODAY.plusDays(5), 6, null, 10, 0);
            Task e2 = s.addExam("Strong", "E2", TODAY.plusDays(5), 6, null, 10, 0);
            check(e1.getReadinessGap() > e2.getReadinessGap(), "weak subject should have bigger gap");
            check(s.getRankedTasks().get(0).getTask().equals(e1), "unprepared exam ranks first");
            check(weak.getPreparationLevel() < strong.getPreparationLevel(), "sanity");
        });
        test("revising topics lowers an exam's readiness gap", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 40);
            Exam e = (Exam) s.addExam("Math", "E", TODAY.plusDays(5), 6, null, 10, 0);
            double before = e.getReadinessGap();
            e.reviseTopics(5);
            check(e.getReadinessGap() < before, "gap should drop");
            e.reviseTopics(100);
            eq(10, e.getRevisedTopics(), "revised topics cap at total");
        });

        // ---- progress tracking
        test("logging hours updates task and subject progress", () -> {
            StudyFlowService s = fresh();
            Subject m = s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(5), 8, null, 10);
            s.logSession(t.getId(), 4, TODAY);
            near(50.0, t.getProgressPercent(), "task progress");
            near(50.0, m.getProgressPercent(), "subject progress");
            eq(Status.IN_PROGRESS, t.getStatus(), "status after partial work");
            s.logSession(t.getId(), 4, TODAY);
            eq(Status.COMPLETED, t.getStatus(), "status after full work");
            near(100.0, m.getProgressPercent(), "subject progress at the end");
        });
        test("cannot log hours on a completed task", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(5), 2, null, 10);
            s.completeTask(t.getId());
            expect(InvalidInputException.class, () -> s.logSession(t.getId(), 1, TODAY));
            expect(InvalidInputException.class, () -> s.completeTask(t.getId()));
        });
        test("subject progress is weighted by task size", () -> {
            StudyFlowService s = fresh();
            Subject m = s.addSubject("Math", 3, 50);
            Task small = s.addAssignment("Math", "Small", TODAY.plusDays(5), 2, null, 10);
            s.addAssignment("Math", "Big", TODAY.plusDays(5), 8, null, 10);
            s.completeTask(small.getId());
            near(20.0, m.getProgressPercent(), "2 of 10 hours done");
        });

        // ---- priority strategies
        test("closer deadline gives higher priority", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task near = s.addAssignment("Math", "Near", TODAY.plusDays(2), 4, null, 10);
            Task far = s.addAssignment("Math", "Far", TODAY.plusDays(10), 4, null, 10);
            WeightedPriorityStrategy w = new WeightedPriorityStrategy();
            check(w.calculate(near, TODAY) > w.calculate(far, TODAY), "near > far");
        });
        test("completed tasks score zero and are not ranked", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(1), 4, null, 10);
            s.completeTask(t.getId());
            near(0.0, new WeightedPriorityStrategy().calculate(t, TODAY), "score");
            check(s.getRankedTasks().isEmpty(), "ranking should be empty");
        });
        test("overdue tasks get a boost over tasks due today", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task late = s.addAssignment("Math", "Late", TODAY.minusDays(1), 2, null, 10);
            Task today = s.addAssignment("Math", "Today", TODAY, 2, null, 10);
            WeightedPriorityStrategy w = new WeightedPriorityStrategy();
            check(w.calculate(late, TODAY) > w.calculate(today, TODAY), "overdue boost");
        });
        test("scores always stay within 0-100", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 5, 0);
            Task t = s.addExam("Math", "Huge", TODAY.minusDays(30), 300, null, 500, 0);
            for (com.studyflow.strategy.PriorityStrategy ps : com.studyflow.strategy.PriorityEngine.availableStrategies()) {
                double v = ps.calculate(t, TODAY);
                check(v >= 0 && v <= 100, ps.getName() + " out of range: " + v);
            }
        });
        test("priority levels map to bands", () -> {
            eq(PriorityLevel.CRITICAL, PriorityLevel.fromScore(85), "85");
            eq(PriorityLevel.HIGH, PriorityLevel.fromScore(55), "55");
            eq(PriorityLevel.MEDIUM, PriorityLevel.fromScore(30), "30");
            eq(PriorityLevel.LOW, PriorityLevel.fromScore(5), "5");
        });
        test("switching strategy at run time changes the ranking", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Hard", 5, 50);
            s.addSubject("Easy", 1, 50);
            Task hardFar = s.addAssignment("Hard", "HardFar", TODAY.plusDays(14), 10, null, 10);
            Task easyNear = s.addAssignment("Easy", "EasyNear", TODAY.plusDays(1), 1, null, 10);
            s.setStrategy(new DeadlineFirstStrategy());
            eq(easyNear, s.getRankedTasks().get(0).getTask(), "deadline-first picks the near task");
            s.setStrategy(new HardestFirstStrategy());
            eq(hardFar, s.getRankedTasks().get(0).getTask(), "hardest-first picks the hard task");
        });
        test("ranking is sorted descending", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            List<RankedTask> r = s.getRankedTasks();
            for (int i = 1; i < r.size(); i++) {
                check(r.get(i - 1).getScore() >= r.get(i).getScore(), "not sorted at " + i);
            }
        });
        test("tasks sort by deadline using Comparable", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            List<Task> sorted = new ArrayList<>(s.getTasks());
            Collections.sort(sorted);
            for (int i = 1; i < sorted.size(); i++) {
                check(!sorted.get(i - 1).getDueDate().isAfter(sorted.get(i).getDueDate()), "order");
            }
        });

        // ---- scheduler
        test("scheduler never exceeds the daily capacity", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            Schedule plan = s.generateSchedule(10);
            for (int i = 0; i < 10; i++) {
                check(plan.getHoursOn(TODAY.plusDays(i)) <= 4.0 + 1e-9, "day " + i + " overfilled");
            }
        });
        test("scheduler never plans work after a deadline", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            for (ScheduleEntry e : s.generateSchedule(14).getEntries()) {
                check(!e.getDate().isAfter(e.getTask().getDueDate()), e + " is after its deadline");
            }
        });
        test("scheduler never plans more than the remaining work", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            Schedule plan = s.generateSchedule(14);
            for (Task t : s.getTasks()) {
                check(plan.getHoursFor(t) <= t.getRemainingHours() + 1e-9, t.getTitle() + " over-planned");
            }
        });
        test("scheduler schedules everything when it can all fit", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            Schedule plan = s.generateSchedule(7);
            check(plan.getAtRisk().isEmpty(), "demo data is feasible but got warnings: " + plan.getAtRisk());
            for (Task t : s.getTasks()) {
                if (t.getDueDate().isBefore(TODAY.plusDays(7))) {
                    near(t.getRemainingHours(), plan.getHoursFor(t), t.getTitle() + " fully planned");
                }
            }
        });
        test("scheduler flags impossible deadlines", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task t = s.addExam("Math", "Impossible", TODAY.plusDays(1), 20, null, 10, 0);
            Schedule plan = s.generateSchedule(5);
            check(plan.getAtRisk().containsKey(t), "should be flagged");
            check(plan.getAtRisk().get(t) > 10, "most of the work does not fit");
        });
        test("near deadlines are not starved by a higher-priority distant task", () -> {
            // 4h/day, max 2h per task per day. B and C (6h each, due in 2 days) need every slot of the
            // next 3 days: 2h each, every day. A pure "highest score first" rule would give the
            // top slot to the distant exam instead and make one of them impossible.
            StudyFlowService s = fresh();
            s.addSubject("Math", 5, 10);
            s.addSubject("Art", 1, 90);
            s.addExam("Math", "Distant exam", TODAY.plusDays(10), 12, null, 20, 0);
            Task b = s.addAssignment("Art", "Near B", TODAY.plusDays(2), 6, null, 5);
            Task c = s.addAssignment("Art", "Near C", TODAY.plusDays(2), 6, null, 5);
            Schedule plan = s.generateSchedule(10);
            near(6.0, plan.getHoursFor(b), "B fully planned");
            near(6.0, plan.getHoursFor(c), "C fully planned");
            check(plan.getAtRisk().isEmpty(), "no warnings expected but got " + plan.getAtRisk());
        });
        test("scheduler uses the whole available time when work remains", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            s.addProject("Math", "Big", TODAY.plusDays(20), 100, null, 1);
            s.addProject("Math", "Big2", TODAY.plusDays(20), 100, null, 1);
            Schedule plan = s.generateSchedule(3);
            near(12.0, plan.getTotalHours(), "3 days x 4h");
        });
        test("scheduler validates its settings", () -> {
            StudyFlowService s = fresh();
            expect(InvalidInputException.class, () -> s.getScheduler().setDailyHours(0));
            expect(InvalidInputException.class, () -> s.getScheduler().setDailyHours(20));
            expect(InvalidInputException.class, () -> s.generateSchedule(0));
        });

        // ---- repository / service
        test("unknown task id raises EntityNotFoundException", () ->
                expect(EntityNotFoundException.class, () -> fresh().findTask("T999")));
        test("deleting a task removes its sessions", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(5), 8, null, 10);
            s.logSession(t.getId(), 2, TODAY);
            s.removeTask(t.getId());
            eq(0, s.getTasks().size(), "tasks");
            eq(0, s.getSessions().size(), "sessions");
            eq(0, s.findSubject("Math").getTasks().size(), "subject's tasks");
        });
        test("deleting a subject cascades to its tasks", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            s.removeSubject("Math");
            check(s.getTasks().stream().noneMatch(t -> t.getSubject().getName().equals("Math")), "math tasks gone");
            eq(3, s.getSubjects().size(), "subjects left");
        });
        test("rescheduling a task changes its deadline", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            Task t = s.addAssignment("Math", "HW", TODAY.plusDays(5), 8, null, 10);
            s.rescheduleTask(t.getId(), TODAY.plusDays(9));
            eq(TODAY.plusDays(9), t.getDueDate(), "new due date");
        });

        // ---- reports
        test("overdue work makes a subject AT_RISK", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            s.addAssignment("Math", "Late", TODAY.minusDays(2), 3, null, 10);
            eq(HealthStatus.AT_RISK, s.getSubjectReports().get(0).getStatus(), "status");
            eq(1, s.getSummary().getOverdueTasks(), "overdue count");
        });
        test("summary counts hours and completion", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            check(s.getSummary().getTotalHours() > 0, "hours");
            check(s.getSummary().getHoursLast7Days() > 0, "weekly hours");
            check(s.getSummary().getFocusSubject().isPresent(), "focus subject");
            near(0.0, s.getSummary().getCompletionRate(), "nothing completed yet");
        });

        // ---- observer
        test("listeners are told about completions and deadlines", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math", 3, 50);
            final int[] counts = new int[3];
            s.addListener(new StudyEventListener() {
                @Override
                public void onTaskCompleted(Task t) {
                    counts[0]++;
                }

                @Override
                public void onDeadlineApproaching(Task t, long days) {
                    counts[1]++;
                }

                @Override
                public void onTaskOverdue(Task t, long days) {
                    counts[2]++;
                }
            });
            Task a = s.addAssignment("Math", "Soon", TODAY.plusDays(1), 2, null, 10);
            s.addAssignment("Math", "Late", TODAY.minusDays(3), 2, null, 10);
            s.addAssignment("Math", "Later", TODAY.plusDays(30), 2, null, 10);
            eq(2, s.checkDeadlines(3).size(), "tasks within 3 days");
            eq(1, counts[1], "approaching events");
            eq(1, counts[2], "overdue events");
            s.logSession(a.getId(), 2, TODAY);
            eq(1, counts[0], "completion events");
        });

        // ---- persistence
        test("save and load preserve everything", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            Path file = Files.createTempFile("sf-test", ".txt");
            try {
                FileDataStore store = new FileDataStore(file.toString());
                store.save(s.exportData());
                StudyFlowService copy = fresh();
                copy.importData(store.load());
                eq(s.getSubjects().size(), copy.getSubjects().size(), "subjects");
                eq(s.getTasks().size(), copy.getTasks().size(), "tasks");
                eq(s.getSessions().size(), copy.getSessions().size(), "sessions");
                for (Task original : s.getTasks()) {
                    Task loaded = copy.findTask(original.getId());
                    eq(original.getType(), loaded.getType(), "type");
                    eq(original.getTitle(), loaded.getTitle(), "title");
                    eq(original.getDueDate(), loaded.getDueDate(), "due");
                    eq(original.getStatus(), loaded.getStatus(), "status");
                    near(original.getHoursDone(), loaded.getHoursDone(), "hours done");
                    eq(original.getDetails(), loaded.getDetails(), "details");
                }
                near(s.findSubject("Math").getProgressPercent(),
                        copy.findSubject("Math").getProgressPercent(), "subject progress");
                near(s.getRankedTasks().get(0).getScore(), copy.getRankedTasks().get(0).getScore(), "top score");
            } finally {
                Files.deleteIfExists(file);
            }
        });
        test("special characters in titles survive a save/load cycle", () -> {
            StudyFlowService s = fresh();
            s.addSubject("Math | Stats", 3, 50);
            String title = "Report | draft % final\nline2";
            s.addAssignment("Math | Stats", title, TODAY.plusDays(5), 3, null, 10);
            Path file = Files.createTempFile("sf-test", ".txt");
            try {
                FileDataStore store = new FileDataStore(file.toString());
                store.save(s.exportData());
                StudyFlowService copy = fresh();
                copy.importData(store.load());
                eq(title, copy.getTasks().get(0).getTitle(), "title");
                eq("Math | Stats", copy.getSubjects().get(0).getName(), "subject name");
            } finally {
                Files.deleteIfExists(file);
            }
            eq("a%7Cb%25", TextUtil.escape("a|b%"), "escape");
        });
        test("ids stay unique after loading", () -> {
            StudyFlowService s = fresh();
            DemoData.populate(s);
            Path file = Files.createTempFile("sf-test", ".txt");
            try {
                FileDataStore store = new FileDataStore(file.toString());
                store.save(s.exportData());
                IdGenerator.reset();                                  // simulate a new program run
                StudyFlowService copy = new StudyFlowService();
                copy.importData(store.load());
                Task fresh = copy.addAssignment("Math", "New", LocalDate.now().plusDays(3), 2, null, 5);
                long same = copy.getTasks().stream().filter(t -> t.getId().equals(fresh.getId())).count();
                eq(1L, same, "new id must not collide");
            } finally {
                Files.deleteIfExists(file);
            }
        });
        test("missing file loads as empty data", () -> {
            FileDataStore store = new FileDataStore("definitely-not-here-" + System.nanoTime() + ".txt");
            check(!store.exists() && store.load().isEmpty(), "empty");
        });
        test("corrupted file raises StorageException", () -> {
            Path file = Files.createTempFile("sf-test", ".txt");
            try {
                Files.write(file, Arrays.asList("# header", "SUBJECT|S001|Math|notANumber|50"), StandardCharsets.UTF_8);
                expect(StorageException.class, () -> new FileDataStore(file.toString()).load());
                Files.write(file, Arrays.asList("SUBJECT|S001|Math|3|50",
                        "EXAM|T001|S999|Final|2026-11-01|5.0|3|0.0|PENDING|5|0"), StandardCharsets.UTF_8);
                expect(StorageException.class, () -> new FileDataStore(file.toString()).load());
            } finally {
                Files.deleteIfExists(file);
            }
        });

        System.out.println("---------------");
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
