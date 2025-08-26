package com.example.classifierapi.store;

import com.example.classifierapi.core.ImprovedClassifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral per-session model store.
 * Each modelId lives in memory and expires after 30 minutes of inactivity.
 */
@Component
public class ModelStore {

    private static final long TTL_MILLIS = 30 * 60 * 1000; // 30 min idle
    private final Map<String, Entry> models = new ConcurrentHashMap<>();

    private static final class Entry {
        final ImprovedClassifier model;
        volatile long lastAccess;
        Entry(ImprovedClassifier m) { this.model = m; touch(); }
        void touch() { this.lastAccess = Instant.now().toEpochMilli(); }
    }

    private void cleanup() {
        long now = Instant.now().toEpochMilli();
        models.entrySet().removeIf(e -> now - e.getValue().lastAccess > TTL_MILLIS);
    }

    /** Put a model and return its new modelId */
    public String put(ImprovedClassifier model) {
        cleanup();
        String id = UUID.randomUUID().toString();
        models.put(id, new Entry(model));
        return id;
    }

    /** Get a model by id, or null if not found/expired */
    public ImprovedClassifier get(String id) {
        cleanup();
        Entry e = models.get(id);
        if (e == null) return null;
        e.touch();
        return e.model;
    }

    /** Remove a model by id (optional) */
    public boolean remove(String id) {
        return models.remove(id) != null;
    }
}
