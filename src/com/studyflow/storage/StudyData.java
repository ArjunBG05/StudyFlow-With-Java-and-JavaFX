package com.studyflow.storage;

import com.studyflow.model.StudySession;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;

import java.util.ArrayList;
import java.util.List;

/** Plain container passed between the service and the storage layer. */
public class StudyData {

    private final List<Subject> subjects = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<StudySession> sessions = new ArrayList<>();

    public List<Subject> getSubjects() {
        return subjects;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<StudySession> getSessions() {
        return sessions;
    }

    public boolean isEmpty() {
        return subjects.isEmpty() && tasks.isEmpty() && sessions.isEmpty();
    }
}
