package testutil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Helper class for making HTTP requests to local server during tests
public final class ClientDrivers {
    private final HttpClient client = HttpClient.newHttpClient(); // Java 11 HTTP client
    private final int port; // server port to connect

    public ClientDrivers(int port) { this.port = port; }

    // build base URL
    public static String base(int port) { return "http://127.0.0.1:" + port; }

    // GET /weather.json
    public Response getAll() throws IOException, InterruptedException {
        return get("/weather.json");
    }

    // GET /weather/{id}
    public Response getById(String id) throws IOException, InterruptedException {
        return get("/weather/" + id);
    }

    // PUT /weather.json with JSON body
    public Response putWeather(String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .header("Content-Type", "application/json")
                .header("X-Lamport", "1") // optional Lamport header
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    // PUT raw body with custom content-type
    public Response putRaw(String body, String contentType) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    // POST to /weather.json (used for invalid input tests)
    public Response postToWeather() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    // Generic GET for a given path
    public Response get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + path))
                .GET()
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    // Simple wrapper for HTTP response
    public static final class Response {
        public final int code; // HTTP status code
        public final String body; // response body

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}