package testutil;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TestData {
    private TestData() {}

    /** A valid FLAT weather JSON object with required "id". */
    public static String validFlat(String id) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", "Adelaide (West Terrace)");
        m.put("state", "SA");
        m.put("lat", "-34.9");
        m.put("lon", "138.6");
        m.put("air_temp", "13.0");
        m.put("rel_hum", "60");
        m.put("wind_spd_kmh", "15");
        m.put("wind_spd_kt", "8");
        m.put("wind_dir", "S");
        m.put("dewpt", "5.7");
        m.put("press", "1023.9");
        m.put("cloud", "Partly Rainyyyyy");
        m.put("time_zone", "CST");
        m.put("local_date_time_full", "20230715160000");
        m.put("local_date_time", "15/04:00pm");
        return toJsonObject(m);
    }

    /** Minimal valid object: only id + 1 field, still valid for 201/200. */
    public static String minimalValid(String id) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", "Station");
        return toJsonObject(m);
    }

    /** Missing id -> should be 400. */
    public static String missingId() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", "No ID here");
        return toJsonObject(m);
    }

    /** Malformed payload. */
    public static String malformed() {
        return "not-json";
    }

    private static String toJsonObject(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(e.getKey())).append('"').append(':')
                    .append('"').append(esc(e.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
