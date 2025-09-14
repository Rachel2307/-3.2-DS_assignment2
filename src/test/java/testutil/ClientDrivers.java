package testutil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Simple static HTTP helpers using the JDK client. */
public final class ClientDrivers {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private ClientDrivers() {}

    public static HttpResponse<String> putWeather(String baseUrl, String jsonBody, long lamport) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/weather.json"))
                    .header("Content-Type", "application/json")
                    .header("X-Lamport", Long.toString(lamport))
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse<String> getSnapshot(String baseUrl, long lamport) {
        return get(baseUrl + "/weather.json", lamport);
    }

    public static HttpResponse<String> getById(String baseUrl, String id, long lamport) {
        return get(baseUrl + "/weather/" + id, lamport);
    }

    private static HttpResponse<String> get(String url, long lamport) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Lamport", Long.toString(lamport))
                    .GET()
                    .build();
            return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
