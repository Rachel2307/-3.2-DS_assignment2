package core.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Represents a weather record submitted by a content server
// Stores JSON data fields, timestamp, and Lamport clock for ordering
public class WeatherRecord {
    private final Map<String, Object> fields = new HashMap<>(); // stores key/value data from JSON
    private final Instant receivedAt = Instant.now();           // time when record is created
    private final long lamport;                                 // Lamport timestamp for ordering

    // constructor: copy data from input map and set Lamport
    public WeatherRecord(Map<String, Object> data, long lamport) {
        this.fields.putAll(data);
        this.lamport = lamport;
    }

    @JsonAnyGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getFields() {
        return fields; // return all fields for JSON serialization
    }

    // getter for when the record was received
    public Instant receivedAt() { return receivedAt; }

    // getter for Lamport timestamp
    public long lamport() { return lamport; }

    // get the "id" field from JSON if it exists
    public String id() {
        Object v = fields.get("id");
        return v == null ? null : v.toString();
    }
}
