package com.studyflow.ui;

import com.studyflow.exception.EntityNotFoundException;
import com.studyflow.model.Difficulty;
import com.studyflow.model.Task;
import com.studyflow.service.StudyFlowService;

import java.time.LocalDate;

/** Sample data so the application can be demonstrated immediately. Dates are relative to today. */
public final class DemoData {

    private DemoData() { }

    public static void populate(StudyFlowService svc) throws EntityNotFoundException {
        LocalDate today = svc.today();

        svc.addSubject("Math", 5, 35);
        svc.addSubject("Physics", 4, 60);
        svc.addSubject("English", 2, 80);
        svc.addSubject("CS", 3, 70);

        Task midterm = svc.addExam("Math", "Calculus Midterm", today.plusDays(6), 12, null, 10, 2);
        Task problemSet = svc.addAssignment("Math", "Problem Set 7", today.plusDays(2), 3, null, 10);
        svc.addExam("Physics", "Mechanics Quiz", today.plusDays(4), 5, Difficulty.MEDIUM, 6, 3);
        svc.addAssignment("Physics", "Lab Report", today.plusDays(3), 4, null, 20);
        svc.addAssignment("English", "Essay Draft", today.plusDays(9), 6, null, 25);
        Task webApp = svc.addProject("CS", "Web App Project", today.plusDays(12), 15, null, 3);

        svc.logSession(problemSet.getId(), 1.5, today.minusDays(1));
        svc.logSession(webApp.getId(), 4, today.minusDays(2));
        svc.logSession(midterm.getId(), 2, today.minusDays(1));
    }
}
