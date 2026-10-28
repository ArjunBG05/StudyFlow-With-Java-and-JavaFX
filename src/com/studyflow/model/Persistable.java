package com.studyflow.model;

/** Anything that can be written to the data file as a single text line. */
public interface Persistable {
    String toRecord();
}
