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

// Aggregation service: applies updates from content servers in order
// Handles ingestion, snapshot, and pruning expired data
public final class AggregationService {
    private final StateStore store;             // persistent store for all states
    private final LamportClock clock;           // Lamport clock for ordering
    private final ObjectMapper mapper = new ObjectMapper(); // JSON parser

    private final Map<String, SourceState> state = new ConcurrentHashMap<>(); // id -> SourceState
    private final BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>(); // queue of incoming data
    private Thread writerThread;                // background thread to apply updates

    private Consumer<String> logger = System.out::println; // simple logger

    // constructor: initialize store, clock, recover persisted state, start writer thread
    public AggregationService(StateStore store, LamportClock clock) {
        this.store = store;
        this.clock = clock;

        // try to load previous state, but ignore errors
        try {
            Map<String, SourceState> recovered = store.load();
            if (recovered != null) state.putAll(recovered);
        } catch (IOException e) {
            logger.accept("[startup] state load failed, starting empty: " + e.getMessage());
        }

        // start writer thread to process queue
        writerThread = new Thread(this::writerLoop, "aggregation-writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    // allow setting a custom logger
    public void setLogger(Consumer<String> logger) {
        this.logger = (logger == null) ? (s -> {}) : logger;
    }

    // ingest JSON string, enqueue for writer thread, return 201 if first, 200 if update
    public int ingest(String json, LamportClock ignoredForLamport) throws IllegalArgumentException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String id = (map.get("id") == null ? null : map.get("id").toString());
            if (id == null || id.isBlank()) throw new IllegalArgumentException("missing id");

            // add metadata for tracking
            map.put("_enqueuedAtMs", System.currentTimeMillis());
            map.put("_enqueuedLamport", clock.now());

            queue.add(map);
            return state.containsKey(id) ? 200 : 201;
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid json");
        }
    }

    // background loop to take from queue and apply updates
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

    // apply a single map to state
    private synchronized void applyMap(Map<String, Object> map) {
        String id = String.valueOf(map.get("id"));

        // create WeatherRecord with Lamport timestamp
        WeatherRecord rec = new WeatherRecord(map, clock.now());
        boolean first = !state.containsKey(id);

        // update or insert SourceState
        state.put(id, new SourceState(rec));

        try { persist(); } catch (Exception ignore) {}

        int qSizeAfter = queue.size();
        // just log the PUT event, do not tick Lamport here
        logger.accept(String.format("[PUT] id=%s L=%d q=%d", id, clock.now(), qSizeAfter));
    }

    // snapshot of all sources as JSON array
    public String snapshotJson() throws IOException {
        List<Map<String, Object>> arr = new ArrayList<>();
        for (SourceState s : state.values()) arr.add(s.record().getFields());
        return mapper.writeValueAsString(arr);
    }

    // snapshot for a single id, or null if missing
    public String singleJson(String id) throws IOException {
        SourceState s = state.get(id);
        if (s == null) return null;
        return mapper.writeValueAsString(s.record().getFields());
    }

    // remove entries not seen within expirySec, log removals
    public synchronized void pruneExpired(int expirySec) {
        Instant cutoff = Instant.now().minusSeconds(expirySec);
        List<String> removed = new ArrayList<>();

        // remove expired states
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

    // check if no sources exist
    public boolean isEmpty() {
        return state.isEmpty();
    }

    // save current state to store
    private void persist() throws IOException { store.save(state); }
}
