# Viva preparation: likely questions and short answers

**1. What does your project do?**
It tracks assignments, exams and projects, scores how urgent each is, tracks progress per subject, and generates a day-by-day
study plan that respects daily hours and deadlines.

**2. Where did you use inheritance?**
`Task` is an abstract parent of `Assignment`, `Exam` and `Project`. Also `BasePriorityStrategy` to three strategy classes,
and `StudyFlowException` to `EntityNotFoundException` and `StorageException`.

**3. Where is polymorphism?**
The ranking and scheduler work on `Task` references. Calling `getTypeWeight()` or `getReadinessGap()` runs the
subclass's own version at run time (an exam weighs 1.4; an assignment depends on its grade share). Likewise `strategy.calculate()`
runs whichever strategy is currently selected. Overloading: `calculate(task, today)` and `calculate(task, today, remainingHours)`.

**4. Why is `Task` abstract?**
A generic "task" is never created on its own, only an assignment, exam or project. The abstract class holds the shared state and
logic and forces subclasses to supply the parts that differ (`getType`, `getTypeWeight`, `extraRecord`, `getDetails`).

**5. Abstract class vs interface: why both?**
Interfaces describe capabilities (`Trackable`, `Persistable`, `PriorityStrategy`); a class can implement several.
An abstract class shares code and state (`Task`, `BasePriorityStrategy`). `Task` uses both.

**6. How did you achieve encapsulation?**
Fields are private. Changes go through validated setters (for example `setPreparationLevel` rejects values outside 0-100).
`Subject.getTasks()` returns an unmodifiable list, so outside code cannot bypass `addTask`.

**7. Which design patterns did you use and why?**
Strategy (swap priority rules), Factory (`TaskFactory` builds tasks), Observer (alerts), Template Method
(`Task.toRecord`, `BasePriorityStrategy.calculate`), Facade (`StudyFlowService`), Repository (`Repository<T>`),
and dependency injection (`Clock`, `DataStore`).

**8. Explain the Strategy pattern in your code.**
`PriorityStrategy` is the interface. `WeightedPriorityStrategy`, `DeadlineFirstStrategy`, `HardestFirstStrategy` implement it.
`PriorityEngine` holds one and can switch with `setStrategy()` (menu option 9) without changing the ranking code.

**9. Where did you use generics?**
`Repository<T extends Identifiable>` is a single class that stores subjects, tasks and sessions. The bound lets it call `getId()`.

**10. Checked vs unchecked exceptions: what did you choose?**
Recoverable conditions the caller must handle (missing entity, file problems) are checked. Invalid arguments are unchecked
(`InvalidInputException extends IllegalArgumentException`) so constructors and setters stay clean.

**11. How is the priority calculated?**
A weighted sum of urgency (50%), difficulty (15%), workload (15%) and readiness gap (20%, exams only), multiplied by a
task-type weight, +15 if overdue, clamped to 0-100. The formula is in the README.

**12. How does the scheduler work?**
For each day it first forces in the work that cannot be pushed later without missing a deadline (found by a backward
pass), then fills the remaining time with the highest-priority tasks, re-scoring after each block so subjects interleave.
It never schedules after a deadline and warns about work that cannot fit.

**13. Why a backward pass? Wouldn't greedy be enough?**
No: greedy can let a distant but high-scoring exam take slots a nearer deadline needs, producing a false "impossible"
warning. I found this with the demo data and added a test that fails without the fix.

**14. How is data saved?**
`FileDataStore` writes one record per line. Each model class creates its own record via `Persistable.toRecord()`.
Text is escaped so characters like `|` survive. Loading restores subjects, then tasks, then sessions, and reports corrupted lines.

**15. What is the Observer pattern doing here?**
`StudyFlowService` fires `onTaskCompleted`, `onDeadlineApproaching`, `onTaskOverdue`. `ConsoleNotifier` prints them.
A different listener (file, e-mail, GUI) could be added without changing the service.

**16. Why is a `Clock` passed into the service?**
So tests can fix "today" and get repeatable results (dependency injection).

**17. How did you test it?**
43 tests, including scheduler properties (never exceed daily hours, never after deadline, never over-plan), persistence round
trips, and exception behaviour. I also removed the scheduler's feasibility step to confirm the tests catch the bug.

**18. What are the limitations and how would you extend it?**
Console only; the scheduler is a heuristic, not an exact optimiser; preparation is self-rated. Extensions: GUI (replace `ui`),
database (new `DataStore`), reminders (new listener), automatic preparation updates from quiz scores.

**19. Difference between `equals` and `==`? Did you override them?**
`==` compares references; `equals` compares meaning. `Task`, `Subject` and `StudySession` override `equals`/`hashCode` by id so
they work correctly as map keys (used in `Schedule.atRisk`).

**20. What does `final` do in your code?**
`final` classes for immutable value objects and utilities; `final` fields for ids; `final` setters called from constructors so
subclasses cannot override them and break validation.
