package app;

import app.menu.ConsoleMenu;
import core.LamportClock;
import util.HttpUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Content Server program.
 * - Ticks the Lamport clock once whenever sending a PUT.
 * - After server response, merges Lamport from header.
 * - Reads input file directly and converts to JSON if needed.
 */
public final class ContentServer {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ContentServer <serverBaseUrl> <filePath>");
            System.exit(2);
        }
        String baseUrl = args[0].replaceAll("/+$", "");
        Path filePath = Path.of(args[1]);

        LamportClock clock = new LamportClock("content");
        AtomicReference<Path> fileRef = new AtomicReference<>(filePath);

        // Simple console menu for user interaction
        new ConsoleMenu("Content Server")
                .option("Send PUT to Aggregation Server", () -> {
                    try {
                        // Read file and convert to JSON
                        String json = readAsJson(fileRef.get());

                        // Send PUT (clock ticks once when sending)
                        long L = clock.send();
                        var resp = HttpUtil.putJson(baseUrl + "/weather.json", json, L);

                        // Merge Lamport from server response
                        clock.receive(HttpUtil.readLamport(resp));

                        System.out.printf("PUT status=%d, Lamport=%d%n",
                                resp.statusCode(), clock.now());
                    } catch (Exception e) {
                        System.out.println("PUT failed: " + e.getMessage());
                    }
                })
                .option("Change weather data file", () -> {
                    // Allow switching to a different file path
                    Path newPath = ConsoleMenu.promptPath("New file path", fileRef.get());
                    fileRef.set(newPath);
                    System.out.println("File set to: " + newPath);
                })
                .option("Exit", ConsoleMenu::exit)
                .loop();
    }

    // Read file content. If JSON already, return as-is.
    // If it's key:value format, convert it into JSON.
    private static String readAsJson(Path p) throws IOException {
        String raw = Files.readString(p, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) return ""; // empty body → server should respond with 204
        if (raw.startsWith("{")) return raw;

        Map<String, String> m = parseKeyValue(raw);
        return toJsonFlat(m);
    }

    // Parse key:value lines into a map (ignore empty lines and comments).
    private static Map<String, String> parseKeyValue(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            int i = s.indexOf(':');
            if (i <= 0) continue;
            String k = s.substring(0, i).trim();
            String v = s.substring(i + 1).trim();
            out.put(k, v);
        }
        return out;
    }

    // Build a simple JSON string from a flat map.
    private static String toJsonFlat(Map<String, String> m) {
        if (m == null || m.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(e.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    // Escape quotes and backslashes for JSON.
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
