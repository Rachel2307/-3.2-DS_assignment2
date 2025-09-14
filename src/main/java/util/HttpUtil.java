package util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Small HTTP helper for CLI clients (Java 11+ HttpClient)
public final class HttpUtil {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
            .build(); // shared HTTP client with timeout

    private HttpUtil() {} // prevent instantiation

    // perform GET request with Lamport header
    public static HttpResponse<String> get(String url, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .GET().build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // perform PUT request with JSON body and Lamport header
    public static HttpResponse<String> putJson(String url, String json, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header("Content-Type", "application/json; charset=utf-8")
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // read Lamport value from response header (0 if missing)
    public static long readLamport(HttpResponse<?> resp) {
        try { return Long.parseLong(resp.headers().firstValue(Config.LAMPORT_HEADER).orElse("0")); }
        catch (Exception e) { return 0L; }
    }

    // ensure URL has http:// prefix
    public static String normalize(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        return "http://" + raw;
    }
}
