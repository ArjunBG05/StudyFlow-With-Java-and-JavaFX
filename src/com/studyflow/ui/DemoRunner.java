package com.studyflow.ui;

import com.studyflow.exception.StudyFlowException;
import com.studyflow.service.StudyFlowService;
import com.studyflow.storage.FileDataStore;
import com.studyflow.strategy.PriorityEngine;
import com.studyflow.strategy.PriorityStrategy;

import java.nio.file.Files;
import java.nio.file.Path;

/** Non-interactive walkthrough of every feature (run with: java com.studyflow.Main --demo). */
public final class DemoRunner {

    private DemoRunner() { }

    public static void run() {
        try {
            StudyFlowService svc = new StudyFlowService();
            ReportPrinter printer = new ReportPrinter(System.out);
            svc.addListener(new ConsoleNotifier(System.out));
            DemoData.populate(svc);

            printer.printTasks(svc.getTasksByDeadline(), svc.today());

            for (PriorityStrategy strategy : PriorityEngine.availableStrategies()) {
                svc.setStrategy(strategy);
                printer.printRanking(svc.getRankedTasks(), svc.today(), strategy.getName());
            }
            svc.setStrategy(PriorityEngine.availableStrategies().get(0));

            printer.printReports(svc.getSubjectReports(), svc.getSummary());
            printer.printSchedule(svc.generateSchedule(7));

            printer.heading("Deadline alerts (next 4 days)");
            svc.checkDeadlines(4);

            printer.heading("Persistence round-trip");
            Path file = Files.createTempFile("studyflow-demo", ".txt");
            FileDataStore store = new FileDataStore(file.toString());
            store.save(svc.exportData());
            StudyFlowService reloaded = new StudyFlowService();
            reloaded.importData(store.load());
            System.out.println("Saved to " + file + " and reloaded "
                    + reloaded.getSubjects().size() + " subjects, " + reloaded.getTasks().size()
                    + " tasks, " + reloaded.getSessions().size() + " sessions.");
            Files.deleteIfExists(file);
        } catch (StudyFlowException | java.io.IOException e) {
            System.out.println("Demo failed: " + e.getMessage());
        }
    }
}
