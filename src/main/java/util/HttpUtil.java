package util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small HTTP utility wrapper around Java 11+
 * Used by CLI clients (ContentServer, GETClient) to send GET/PUT requests
 * with Lamport clock headers and proper content negotiation.
 */
public final class HttpUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
            .build(); // shared HTTP client with timeout

    private HttpUtil() {} // prevent instantiation

    /**
     * Perform a GET request with Lamport header.
     */
    public static HttpResponse<String> get(String url, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header("User-Agent", "ATOMClient/1/0")
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .GET()
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Perform a PUT request with JSON body and Lamport header.
     * Required headers:
     *  - User-Agent: ATOMClient/1/0
     *  - Content-Type: application/json; charset=UTF-8
     *  - Content-Length: automatically set by HttpClient
     */
    public static HttpResponse<String> putJson(String url, String json, long lamport) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(normalize(url)))
                .timeout(Duration.ofMillis(Config.HTTP_TIMEOUT_MS))
                .header("User-Agent", "ATOMClient/1/0")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header(Config.LAMPORT_HEADER, Long.toString(lamport))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Extract Lamport clock from response header.
     */
    public static long readLamport(HttpResponse<?> resp) {
        try {
            return Long.parseLong(resp.headers()
                    .firstValue(Config.LAMPORT_HEADER).orElse("0"));
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Ensure URL has a valid protocol.
     */
    public static String normalize(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        return "http://" + raw;
    }
}
