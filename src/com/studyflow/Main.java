package com.studyflow;

import com.studyflow.service.StudyFlowService;
import com.studyflow.storage.FileDataStore;
import com.studyflow.ui.ConsoleApp;
import com.studyflow.ui.DemoRunner;

/**
 * Entry point.
 *   java com.studyflow.Main            interactive menu (data saved in studyflow-data.txt)
 *   java com.studyflow.Main --demo     non-interactive tour of all features
 *   java com.studyflow.Main file.txt   interactive menu using a different data file
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--demo")) {
            DemoRunner.run();
            return;
        }
        String file = args.length > 0 ? args[0] : "studyflow-data.txt";
        new ConsoleApp(new StudyFlowService(), new FileDataStore(file)).run();
    }
}
