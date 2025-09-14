package testutil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class ClientDrivers {
    private final HttpClient client = HttpClient.newHttpClient();
    private final int port;

    public ClientDrivers(int port) { this.port = port; }

    public static String base(int port) { return "http://127.0.0.1:" + port; }

    public Response getAll() throws IOException, InterruptedException {
        return get("/weather.json");
    }

    public Response getById(String id) throws IOException, InterruptedException {
        return get("/weather/" + id);
    }

    public Response putWeather(String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .header("Content-Type", "application/json")
                // Optional Lamport header if your server uses one:
                .header("X-Lamport", "1")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    public Response putRaw(String body, String contentType) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    public Response postToWeather() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + "/weather.json"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    public Response get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base(port) + path))
                .GET()
                .build();
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        return new Response(r.statusCode(), r.body());
    }

    public static final class Response {
        public final int code;
        public final String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
