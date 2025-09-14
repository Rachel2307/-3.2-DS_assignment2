package core.models;

import java.time.Instant;

// Tracks a content server's latest data and when it was last updated
public class SourceState {
    private Instant lastSeen;      // last time we received data
    private WeatherRecord record;  // latest weather record from this source

    // initialize with a weather record and set lastSeen to now
    public SourceState(WeatherRecord record) {
        this.record = record;
        this.lastSeen = Instant.now();
    }

    // update with a new record and refresh lastSeen
    public void update(WeatherRecord newRecord) {
        this.record = newRecord;
        this.lastSeen = Instant.now();
    }

    // get the latest record
    public WeatherRecord record() {
        return record;
    }

    // get the last time this source was updated
    public Instant lastSeen() {
        return lastSeen;
    }
}
