package store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Minimal JSON helper + key:value file parser
// Supports writing Map/List/String/Number/Boolean/null and parsing flat JSON objects
public final class JsonUtil {
    private JsonUtil() {} // prevent instantiation

    // Parse a file with "key:value" lines into a LinkedHashMap (preserves order)
    public static Map<String, String> parseKeyValueFile(Path file) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file)) {
            String s = line == null ? "" : line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            int i = s.indexOf(':');
            if (i <= 0) continue;
            out.put(s.substring(0, i).trim(), s.substring(i + 1).trim());
        }
        return out;
    }

    // Serialize a Map (possibly nested) to JSON string
    public static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, map);
        return sb.toString();
    }

    // Serialize a List to JSON string
    public static String toJsonList(List<?> list) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, list);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof String s) { sb.append('"').append(escape(s)).append('"'); return; }
        if (v instanceof Number || v instanceof Boolean) { sb.append(v.toString()); return; }
        if (v instanceof Map<?,?> m) {
            sb.append('{'); boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (v instanceof List<?> list) {
            sb.append('['); boolean first = true;
            for (Object it : list) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, it);
            }
            sb.append(']');
            return;
        }
        // fallback: convert to JSON string
        sb.append('"').append(escape(String.valueOf(v))).append('"');
    }

    private static String escape(String s) {
        // escape quotes, backslash, newline, carriage return, tab
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t");
    }

    // Small parser for flat JSON objects (no nested arrays/objects)
    public static Map<String, Object> parseJsonObj(String json) {
        if (json == null) throw new IllegalArgumentException("null json");
        String s = json.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) throw new IllegalArgumentException("not an object");
        int i = 1; // skip '{'
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            i = skipWs(s, i);
            if (i >= s.length()) break;
            if (s.charAt(i) == '}') break;
            // key
            if (s.charAt(i) != '"') throw new IllegalArgumentException("expected key string at pos " + i);
            int[] kp = readString(s, i + 1);
            String key = s.substring(kp[0], kp[1]);
            i = kp[2];
            i = skipWs(s, i);
            if (i >= s.length() || s.charAt(i) != ':') throw new IllegalArgumentException("expected ':' after key at pos " + i);
            i++;
            i = skipWs(s, i);
            // value
            ParseResult vr = readValue(s, i);
            out.put(key, vr.value);
            i = skipWs(s, vr.next);
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { break; }
            if (i >= s.length()) break;
        }
        return out;
    }

    private static int skipWs(String s, int i) { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; return i; }

    private static int[] readString(String s, int i) {
        int start = i;
        StringBuilder out = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '\\') {
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    default -> out.append(e);
                }
            } else if (c == '"') {
                return new int[]{start, i - 1, i}; // return start, end, next index
            } else {
                out.append(c);
            }
        }
        throw new IllegalArgumentException("unterminated string");
    }

    private record ParseResult(Object value, int next) {}

    private static ParseResult readValue(String s, int i) {
        i = skipWs(s, i);
        if (i >= s.length()) throw new IllegalArgumentException("unexpected end");
        char c = s.charAt(i);
        if (c == '"') {
            int[] kp = readString(s, i + 1);
            String str = s.substring(kp[0], kp[1]);
            return new ParseResult(str, kp[2]);
        }
        // true/false/null
        if (s.startsWith("true", i)) return new ParseResult(Boolean.TRUE, i + 4);
        if (s.startsWith("false", i)) return new ParseResult(Boolean.FALSE, i + 5);
        if (s.startsWith("null", i)) return new ParseResult(null, i + 4);
        // number (read until comma or closing brace)
        int j = i;
        while (j < s.length()) {
            char ch = s.charAt(j);
            if (ch == ',' || ch == '}' || Character.isWhitespace(ch)) break;
            j++;
        }
        String num = s.substring(i, j);
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E")) return new ParseResult(Double.parseDouble(num), j);
            long asLong = Long.parseLong(num);
            return new ParseResult(asLong, j);
        } catch (NumberFormatException e) {
            return new ParseResult(num, j); // fallback: treat as string
        }
    }

    // Flatten a nested map/list into "path: value" lines for pretty printing
    public static List<String> flatten(Map<String, Object> obj) {
        List<String> out = new ArrayList<>();
        walk("", obj, out);
        return out;
    }

    private static void walk(String prefix, Object v, List<String> out) {
        if (v instanceof Map<?,?> m) {
            for (var e : m.entrySet()) {
                String k = String.valueOf(e.getKey());
                walk(prefix.isEmpty() ? k : prefix + "." + k, e.getValue(), out);
            }
        } else if (v instanceof List<?> list) {
            for (int i=0;i<list.size();i++) walk(prefix+"["+i+"]", list.get(i), out);
        } else {
            out.add(prefix + ": " + String.valueOf(v));
        }
    }
}
