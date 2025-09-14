// src/main/java/core/AggregationService.java
package core;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.models.SourceState;
import core.models.WeatherRecord;
import store.StateStore;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Aggregation service:
 * - Writer thread applies updates in order.
 * - NO Lamport ticking here (handlers already merged once per request).
 * - Expiry does not tick; it only logs with current now().
 */
public final class AggregationService {
    private final StateStore store;
    private final LamportClock clock;
    private final ObjectMapper mapper = new ObjectMapper();

    // id -> SourceState
    private final Map<String, SourceState> state = new ConcurrentHashMap<>();
    private final BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
    private Thread writerThread;

    private Consumer<String> logger = System.out::println;

    public AggregationService(StateStore store, LamportClock clock) {
        this.store = store;
        this.clock = clock;

        // Recover persisted state (if available), but never fail startup
        try {
            Map<String, SourceState> recovered = store.load();
            if (recovered != null) state.putAll(recovered);
        } catch (IOException e) {
            logger.accept("[startup] state load failed, starting empty: " + e.getMessage());
        }

        writerThread = new Thread(this::writerLoop, "aggregation-writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void setLogger(Consumer<String> logger) {
        this.logger = (logger == null) ? (s -> {}) : logger;
    }

    /** Enqueue JSON; writer thread will apply in order. Returns 201 (first) or 200 (update). */
    public int ingest(String json, LamportClock ignoredForLamport) throws IllegalArgumentException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String id = (map.get("id") == null ? null : map.get("id").toString());
            if (id == null || id.isBlank()) throw new IllegalArgumentException("missing id");

            // Tag metadata (informational only)
            map.put("_enqueuedAtMs", System.currentTimeMillis());
            map.put("_enqueuedLamport", clock.now());

            queue.add(map);
            return state.containsKey(id) ? 200 : 201;
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid json");
        }
    }

    private void writerLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Map<String, Object> map = queue.take();
                applyMap(map);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void applyMap(Map<String, Object> map) {
        String id = String.valueOf(map.get("id"));

        // Construct WeatherRecord with the original Object map + current Lamport
        WeatherRecord rec = new WeatherRecord(map, clock.now());
        boolean first = !state.containsKey(id);

        // Your SourceState takes only WeatherRecord
        state.put(id, new SourceState(rec));

        try { persist(); } catch (Exception ignore) {}

        int qSizeAfter = queue.size();
        // DO NOT tick here; just log with current clock.now()
        logger.accept(String.format("[PUT] id=%s L=%d q=%d", id, clock.now(), qSizeAfter));
    }

    /** Snapshot of all sources as JSON array of objects. */
    public String snapshotJson() throws IOException {
        List<Map<String, Object>> arr = new ArrayList<>();
        for (SourceState s : state.values()) arr.add(s.record().getFields());
        return mapper.writeValueAsString(arr);
    }

    /** Snapshot for one id, or null. */
    public String singleJson(String id) throws IOException {
        SourceState s = state.get(id);
        if (s == null) return null;
        return mapper.writeValueAsString(s.record().getFields());
    }

    /** Remove entries that haven't been seen within expirySec; logs removals (no ticking). */
    public synchronized void pruneExpired(int expirySec) {
        Instant cutoff = Instant.now().minusSeconds(expirySec);
        List<String> removed = new ArrayList<>();

        state.entrySet().removeIf(e -> {
            SourceState ss = e.getValue();
            Instant seen = (ss.lastSeen() != null) ? ss.lastSeen() : Instant.EPOCH;
            boolean rm = seen.isBefore(cutoff);
            if (rm) removed.add(e.getKey());
            return rm;
        });

        if (!removed.isEmpty()) {
            logger.accept(String.format("[expiry] removed=%d ids=%s L=%d",
                    removed.size(), removed, clock.now()));
        }
        try { persist(); } catch (Exception ignore) {}
    }

    public boolean isEmpty() {
        return state.isEmpty();
    }

    private void persist() throws IOException { store.save(state); }
}
