package app;

import core.LamportClock;
import util.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class GETClient {
    // If true → don’t send Lamport clock with GET (used for debugging).
    private static final boolean QUIET_GET = false;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: GETClient <serverBaseUrl>");
            System.exit(2);
        }
        String base = args[0].replaceAll("/+$", "");

        LamportClock clock = new LamportClock("get-client");
        HttpClient client = HttpClient.newHttpClient();

        // Build GET request to /weather.json
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(base + "/weather.json"))
                .GET();

        // Tick Lamport once before sending (unless QUIET mode is on)
        if (!QUIET_GET) {
            long L = clock.send();
            rb.header(Config.LAMPORT_HEADER, String.valueOf(L));
        }

        // Send GET request
        HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());

        // Merge Lamport from server response
        long respLam = parseLamportHeader(resp);
        clock.receive(respLam);

        int status = resp.statusCode();
        String lam = String.valueOf(respLam);

        // Print response info
        System.out.printf("Status: %d %s  X-Lamport=%s%n", status, statusText(status), lam);
        System.out.println("--- JSON ---");
        String body = (resp.body() == null) ? "" : resp.body().trim();
        if (status == 204 || body.isBlank()) {
            System.out.println("(no content)");
        } else {
            System.out.println(body);
        }
    }

    // Read Lamport clock from response header
    private static long parseLamportHeader(HttpResponse<?> resp) {
        String v = resp.headers().firstValue(Config.LAMPORT_HEADER).orElse("0");
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return 0L; }
    }

    // Convert status codes into short text
    private static String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
}
