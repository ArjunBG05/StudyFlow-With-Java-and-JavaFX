package com.studyflow.storage;

import com.studyflow.exception.StorageException;

/**
 * Abstraction over persistence. The application depends on this interface,
 * so the file format could be swapped for a database without touching the
 * rest of the code (Dependency Inversion).
 */
public interface DataStore {

    void save(StudyData data) throws StorageException;

    StudyData load() throws StorageException;

    boolean exists();
}
