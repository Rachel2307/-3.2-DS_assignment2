package core.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Weather record for one content server's submission.
 * - Stores the parsed key/value JSON data
 * - Tracks timestamp + Lamport for ordering
 */
public class WeatherRecord {
    private final Map<String, Object> fields = new HashMap<>();
    private final Instant receivedAt = Instant.now();
    private final long lamport;

    public WeatherRecord(Map<String, Object> data, long lamport) {
        this.fields.putAll(data);
        this.lamport = lamport;
    }

    @JsonAnyGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getFields() {
        return fields;
    }

    public Instant receivedAt() { return receivedAt; }
    public long lamport() { return lamport; }

    public String id() {
        Object v = fields.get("id");
        return v == null ? null : v.toString();
    }
}
