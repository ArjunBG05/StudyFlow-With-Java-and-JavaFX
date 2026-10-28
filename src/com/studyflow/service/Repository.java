package com.studyflow.service;

import com.studyflow.exception.EntityNotFoundException;
import com.studyflow.exception.InvalidInputException;
import com.studyflow.model.Identifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic in-memory store. One class serves Subjects, Tasks and Sessions:
 * T is bounded by Identifiable so the repository can index by id.
 */
public class Repository<T extends Identifiable> {

    private final String entityName;
    private final Map<String, T> items = new LinkedHashMap<>();

    public Repository(String entityName) {
        this.entityName = entityName;
    }

    public void add(T item) {
        if (items.containsKey(item.getId())) {
            throw new InvalidInputException(entityName + " already exists: " + item.getId());
        }
        items.put(item.getId(), item);
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public T getById(String id) throws EntityNotFoundException {
        return findById(id).orElseThrow(() -> new EntityNotFoundException(entityName, id));
    }

    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public List<T> findWhere(Predicate<T> filter) {
        List<T> result = new ArrayList<>();
        for (T item : items.values()) {
            if (filter.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public boolean remove(String id) {
        return items.remove(id) != null;
    }

    public int removeIf(Predicate<T> filter) {
        int before = items.size();
        items.values().removeIf(filter);
        return before - items.size();
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
