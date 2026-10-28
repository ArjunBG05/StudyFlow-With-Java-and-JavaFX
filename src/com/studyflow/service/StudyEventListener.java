package com.studyflow.service;

import com.studyflow.model.Task;

/**
 * Observer pattern: the service publishes events, listeners react. The
 * service never needs to know whether alerts go to the console, a file or a GUI.
 * Default methods let a listener implement only the events it cares about.
 */
public interface StudyEventListener {

    default void onTaskCompleted(Task task) { }

    default void onDeadlineApproaching(Task task, long daysLeft) { }

    default void onTaskOverdue(Task task, long daysOverdue) { }
}
