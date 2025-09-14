package util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small HTTP helper for CLI clients (uses Java 11+ HttpClient).
 */
public final class HttpUtil {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
            .build();

    private HttpUtil() {}

    public static HttpResponse<String> get(String url, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .GET().build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> putJson(String url, String json, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header("Content-Type", "application/json; charset=utf-8")
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static long readLamport(HttpResponse<?> resp) {
        try { return Long.parseLong(resp.headers().firstValue(Config.LAMPORT_HEADER).orElse("0")); }
        catch (Exception e) { return 0L; }
    }

    public static String normalize(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        return "http://" + raw;
    }
}
