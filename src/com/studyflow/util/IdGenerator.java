package com.studyflow.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Generates ids such as S001, T014. Counters are advanced when data is loaded from disk. */
public final class IdGenerator {

    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    private IdGenerator() { }

    public static String next(String prefix) {
        int n = COUNTERS.computeIfAbsent(prefix, k -> new AtomicInteger()).incrementAndGet();
        return String.format("%s%03d", prefix, n);
    }

    /** Makes sure future ids never collide with an id that was loaded from storage. */
    public static void observe(String id) {
        String prefix = id.replaceAll("\\d+$", "");
        String digits = id.substring(prefix.length());
        if (digits.isEmpty()) {
            return;
        }
        int n = Integer.parseInt(digits);
        COUNTERS.computeIfAbsent(prefix, k -> new AtomicInteger()).accumulateAndGet(n, Math::max);
    }

    public static void reset() {
        COUNTERS.clear();
    }
}
