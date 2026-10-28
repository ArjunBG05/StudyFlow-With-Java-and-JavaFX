# StudyFlow: Intelligent Student Productivity System
### Object-Oriented Programming (Java) Course Project Report

> Fill in your name, roll number, course, instructor and date on the title page before submitting.

---

## 1. Abstract

Students usually manage assignments, exams, study schedules and deadlines in separate places, which makes it hard to
decide what to work on first. StudyFlow is an object-oriented Java application that keeps these in one system. It records
academic tasks and study sessions, calculates a priority score for each task from its deadline, difficulty, remaining workload and (for
exams) the student's level of preparation, monitors progress per subject, and generates a day-by-day study plan that
respects the student's available hours and every deadline. The design uses abstraction, inheritance, polymorphism,
generics and five design patterns, and is verified by 43 automated tests.

## 2. Problem statement

Students often manage assignments, exams, study schedules and deadlines separately, making it difficult to determine what
they should focus on first. The system should track academic tasks and study sessions, calculate task priority, monitor
subject progress, and generate personalised study schedules based on deadlines, difficulty and preparation level.

## 3. Objectives

1. Model subjects, assignments, exams, projects and study sessions as a clean class hierarchy.
2. Compute an explainable priority score, and allow the scoring rule to change at run time.
3. Track progress per subject and overall, and flag subjects that are at risk.
4. Generate a study plan that never schedules work after its deadline and warns when a deadline is impossible.
5. Persist data between runs, and validate all input.
6. Apply and demonstrate the core OOP principles and design patterns, and verify them with tests.

## 4. Requirements

**Functional:** add/edit/delete subjects and tasks; log study time; rank tasks; show progress; generate a plan for
1 to 30 days with configurable daily hours; alert on approaching and overdue deadlines; save, load and export.

**Non-functional:** no external dependencies; runs on JDK 11+; clear error messages instead of crashes; readable data file;
code organised in packages with one responsibility per class.

## 5. System design

### 5.1 Architecture

Layered, with dependencies pointing downwards:

```
ui (ConsoleApp, ReportPrinter, ConsoleNotifier)
   v
service (StudyFlowService facade, StudyScheduler, ProgressTracker, Repository<T>)
   v                       v
strategy (PriorityEngine)  storage (DataStore, FileDataStore)
   v                       v
model (Task, Subject, StudySession, enums, interfaces)   +   exception, util, factory
```

The UI contains no business rules. The model knows nothing about the UI or storage.

### 5.2 Class design

See the class diagram in `README.md`. Main decisions:

* `Task` is abstract; `Assignment`, `Exam`, `Project` override the behaviour that differs (type weight, readiness,
  details, extra data to save).
* `Subject` aggregates its tasks and measures progress weighted by task size.
* `PriorityStrategy` is an interface; the engine holds the current strategy (Strategy pattern).
* `Repository<T extends Identifiable>` is a generic in-memory store.
* `StudyFlowService` is a Facade over repositories, engine, scheduler and tracker; it publishes events to
  `StudyEventListener`s (Observer).
* `TaskFactory` creates tasks (Factory); `DataStore` abstracts persistence (Dependency Inversion).

### 5.3 OOP principles applied

See the table in `README.md` section 4. In short: encapsulation (private state, validated setters, unmodifiable views),
abstraction (abstract class and interfaces), inheritance (task hierarchy, strategy hierarchy, exception hierarchy),
polymorphism (overriding in tasks and strategies, overloading in `calculate`), generics, enums with behaviour,
custom checked and unchecked exceptions, and immutable value objects.

### 5.4 Priority algorithm

```
score = 100 x (0.50 urgency + 0.15 difficulty + 0.15 workload + 0.20 readiness) x type weight   (+15 if overdue, max 100)
```

| Factor | Meaning |
|---|---|
| urgency | `1/(1+0.35 x days left)`; 1.0 when due or overdue; increased if more than 3 h/day are needed |
| difficulty | level / 5 |
| workload | remaining hours / 12, capped at 1 |
| readiness | exams only: `1 - (0.6 x preparation + 0.4 x topics revised)`; otherwise its weight goes to urgency |
| type weight | Exam 1.4; Assignment 0.9 to 1.3 by grade share; solo project 1.1; team project 1.0 |

Two other strategies are provided: **Deadline first** (earliest-deadline-first) and **Hardest first**.

### 5.5 Scheduling algorithm

For each day: (1) a backward pass pushes each task's work as late as its deadline allows, latest deadline first; the
work that cannot be pushed later is "must-do today"; (2) the day is filled by repeatedly choosing a must-do task first,
otherwise the highest-priority task (re-scored with hours already planned), in blocks of at most 2 h in half-hour
slots. Tasks are never scheduled after their deadline; unplaceable hours are reported.

**Why the backward pass:** a purely greedy plan can let a high-priority but distant exam take slots needed by nearer
deadlines, so it would wrongly report that a feasible plan is impossible. The pass fixes that; a test (`near deadlines are not
starved...`) fails if it is removed.

### 5.6 Persistence

One text file, one record per line, fields separated by `|`, with `|`, `%` and line breaks escaped. Each model class
creates its own record (`Persistable`); loading proceeds subjects, then tasks, then sessions. Errors are reported with the
line number.

### 5.7 Error handling

| Kind | Class | Example |
|---|---|---|
| Invalid argument (unchecked) | `InvalidInputException` | blank name, difficulty 9, negative hours, duplicate subject |
| Not found (checked) | `EntityNotFoundException` | unknown task id or subject |
| I/O (checked) | `StorageException` | unreadable or corrupted data file |

The console catches these and prints a message, so bad input never crashes the program.

## 6. Implementation summary

| Package | Contents |
|---|---|
| `model` (13) | `Task`, `Assignment`, `Exam`, `Project`, `Subject`, `StudySession`, 4 enums, 3 interfaces |
| `strategy` (7) | `PriorityStrategy`, `BasePriorityStrategy`, 3 strategies, `PriorityEngine`, `RankedTask` |
| `service` (10) | `StudyFlowService`, `StudyScheduler`, `Schedule`, `ScheduleEntry`, `ProgressTracker`, reports, `Repository`, `StudyEventListener` |
| `storage` (3) | `DataStore`, `FileDataStore`, `StudyData` |
| `ui` (6) | `ConsoleApp`, `ConsoleInput`, `ReportPrinter`, `ConsoleNotifier`, `DemoData`, `DemoRunner` |
| `factory`, `exception`, `util` | `TaskFactory`; 4 exception classes; 4 utility classes |

49 source files and about 2,900 lines including comments; 518 lines of tests.

## 7. Testing

43 automated tests (`./test.sh`):

| Area | Tests |
|---|---|
| Validation and encapsulation | 7 |
| Inheritance and polymorphism, exam readiness | 5 |
| Progress tracking | 3 |
| Priority rules and strategies | 8 |
| Scheduler | 8 |
| Service, repository, reports, observer | 7 |
| Persistence | 5 |

**Result:** 43 passed, 0 failed. Scheduler tests were validated by mutation: removing the feasibility pass makes two tests fail.
A scripted run of the interactive menu with invalid input (letters instead of numbers, impossible dates) confirmed that the
program re-prompts instead of crashing.

### Sample output (from `--demo`, data relative to the run date)

```
== Priority ranking ==
Strategy: Balanced (default)
#   Score  Level     Type        Subject    Task                     Due               Left
1   81.0   CRITICAL  Exam        Math       Calculus Midterm         Tue 03 Nov (6d)   10.0h
2   62.8   HIGH      Exam        Physics    Mechanics Quiz           Sun 01 Nov (4d)   5.0h
3   54.6   HIGH      Assignment  Math       Problem Set 7            Fri 30 Oct (2d)   1.5h
4   50.1   HIGH      Assignment  Physics    Lab Report               Sat 31 Oct (3d)   4.0h
5   36.2   MEDIUM    Project     CS         Web App Project          Mon 09 Nov (12d)  11.0h
6   30.4   MEDIUM    Assignment  English    Essay Draft              Fri 06 Nov (9d)   6.0h
```

*(Replace with your own screenshots when submitting: run `./run.sh --demo` and the interactive menu.)*

## 8. Conclusion

StudyFlow turns a scattered set of academic obligations into one prioritised, deadline-aware plan. The project shows how
OOP structure (abstract tasks, interchangeable priority strategies, observers, a facade and a generic repository)
keeps the code extensible: a new task type or priority rule can be added without changing existing logic.

## 9. Future scope

Graphical interface (only the `ui` package changes); database storage (a new `DataStore`); e-mail or mobile reminders (a new
`StudyEventListener`); recurring tasks; Pomodoro breaks; updating preparation levels automatically from past quiz scores;
an exact optimisation of the schedule.
