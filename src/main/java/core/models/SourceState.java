package core.models;

import java.time.Instant;

/**
 * Tracks liveness of a source (content server).
 * Used by expiry to prune old data.
 */
public class SourceState {
    private Instant lastSeen;
    private WeatherRecord record;

    public SourceState(WeatherRecord record) {
        this.record = record;
        this.lastSeen = Instant.now();
    }

    public void update(WeatherRecord newRecord) {
        this.record = newRecord;
        this.lastSeen = Instant.now();
    }

    public WeatherRecord record() { return record; }
    public Instant lastSeen() { return lastSeen; }
}
