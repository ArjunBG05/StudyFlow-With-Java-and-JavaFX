package com.studyflow.factory;

import com.studyflow.exception.StorageException;
import com.studyflow.model.Assignment;
import com.studyflow.model.Difficulty;
import com.studyflow.model.Exam;
import com.studyflow.model.Project;
import com.studyflow.model.Status;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.model.TaskType;
import com.studyflow.util.IdGenerator;
import com.studyflow.util.TextUtil;

import java.time.LocalDate;
import java.util.Map;

/**
 * Factory pattern: the single place that knows how to build each concrete
 * Task. Client code asks for "an exam" and never calls constructors directly.
 * A task with no difficulty of its own inherits its subject's difficulty.
 */
public final class TaskFactory {

    private static final String PREFIX = "T";

    private TaskFactory() { }

    public static Assignment createAssignment(Subject subject, String title, LocalDate due,
                                              double hours, Difficulty difficulty, int gradeWeight) {
        return new Assignment(IdGenerator.next(PREFIX), title, subject, due, hours,
                orSubjectDefault(difficulty, subject), gradeWeight);
    }

    public static Exam createExam(Subject subject, String title, LocalDate due, double hours,
                                  Difficulty difficulty, int totalTopics, int revisedTopics) {
        return new Exam(IdGenerator.next(PREFIX), title, subject, due, hours,
                orSubjectDefault(difficulty, subject), totalTopics, revisedTopics);
    }

    public static Project createProject(Subject subject, String title, LocalDate due,
                                        double hours, Difficulty difficulty, int teamSize) {
        return new Project(IdGenerator.next(PREFIX), title, subject, due, hours,
                orSubjectDefault(difficulty, subject), teamSize);
    }

    /**
     * Rebuilds a task from a saved record.
     * Layout: TYPE|id|subjectId|title|due|estimatedHours|difficulty|hoursDone|status|extra...
     */
    public static Task fromFields(String[] f, Map<String, Subject> subjectsById) throws StorageException {
        try {
            TaskType type = TaskType.valueOf(f[0]);
            Subject subject = subjectsById.get(f[2]);
            if (subject == null) {
                throw new StorageException("Task " + f[1] + " refers to unknown subject " + f[2]);
            }
            String id = f[1];
            String title = TextUtil.unescape(f[3]);
            LocalDate due = LocalDate.parse(f[4]);
            double hours = Double.parseDouble(f[5]);
            Difficulty difficulty = Difficulty.fromLevel(Integer.parseInt(f[6]));

            Task task;
            switch (type) {
                case ASSIGNMENT:
                    task = new Assignment(id, title, subject, due, hours, difficulty, Integer.parseInt(f[9]));
                    break;
                case EXAM:
                    task = new Exam(id, title, subject, due, hours, difficulty,
                            Integer.parseInt(f[9]), Integer.parseInt(f[10]));
                    break;
                case PROJECT:
                    task = new Project(id, title, subject, due, hours, difficulty, Integer.parseInt(f[9]));
                    break;
                default:
                    throw new StorageException("Unknown task type " + type);
            }
            task.restoreState(Double.parseDouble(f[7]), Status.valueOf(f[8]));
            IdGenerator.observe(id);
            return task;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException
                 | java.time.format.DateTimeParseException e) {
            throw new StorageException("Invalid task record: " + e.getMessage(), e);
        }
    }

    private static Difficulty orSubjectDefault(Difficulty difficulty, Subject subject) {
        return difficulty != null ? difficulty : subject.getDifficulty();
    }
}
