package com.studyflow.storage;

import com.studyflow.exception.StorageException;
import com.studyflow.factory.TaskFactory;
import com.studyflow.model.StudySession;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.util.IdGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores everything in one human-readable text file, one record per line.
 * Each model class knows how to turn itself into a record (Persistable);
 * this class only handles the file and the order of loading
 * (subjects first, then tasks, then sessions).
 */
public class FileDataStore implements DataStore {

    private static final String HEADER = "# StudyFlow data file v1";

    private final Path path;

    public FileDataStore(String fileName) {
        this.path = Paths.get(fileName);
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public void save(StudyData data) throws StorageException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Subject s : data.getSubjects()) {
            lines.add(s.toRecord());
        }
        for (Task t : data.getTasks()) {
            lines.add(t.toRecord());
        }
        for (StudySession s : data.getSessions()) {
            lines.add(s.toRecord());
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not save data to " + path, e);
        }
    }

    @Override
    public StudyData load() throws StorageException {
        StudyData data = new StudyData();
        if (!exists()) {
            return data;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not read " + path, e);
        }

        List<String[]> taskRows = new ArrayList<>();
        List<String[]> sessionRows = new ArrayList<>();
        List<Integer> taskLines = new ArrayList<>();
        List<Integer> sessionLines = new ArrayList<>();
        Map<String, Subject> subjects = new LinkedHashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            try {
                if (f[0].equals("SUBJECT")) {
                    Subject s = Subject.fromFields(f);
                    IdGenerator.observe(s.getId());
                    subjects.put(s.getId(), s);
                } else if (f[0].equals("SESSION")) {
                    sessionRows.add(f);
                    sessionLines.add(i + 1);
                } else {
                    taskRows.add(f);
                    taskLines.add(i + 1);
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                throw new StorageException("Corrupted data at line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        data.getSubjects().addAll(subjects.values());

        Map<String, Task> tasks = new LinkedHashMap<>();
        for (int i = 0; i < taskRows.size(); i++) {
            try {
                Task t = TaskFactory.fromFields(taskRows.get(i), subjects);
                t.getSubject().addTask(t);
                tasks.put(t.getId(), t);
            } catch (StorageException e) {
                throw new StorageException("Line " + taskLines.get(i) + ": " + e.getMessage(), e);
            }
        }
        data.getTasks().addAll(tasks.values());

        for (int i = 0; i < sessionRows.size(); i++) {
            try {
                StudySession s = StudySession.fromFields(sessionRows.get(i), tasks);
                IdGenerator.observe(s.getId());
                data.getSessions().add(s);
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException
                     | java.time.format.DateTimeParseException e) {
                throw new StorageException("Corrupted data at line " + sessionLines.get(i)
                        + ": " + e.getMessage(), e);
            }
        }
        return data;
    }
}
