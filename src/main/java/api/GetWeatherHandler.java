package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler for GET requests that retrieves all stored weather data.
 * Uses Lamport clocks for logical time synchronization in a distributed system.
 */
public final class GetWeatherHandler implements HttpHandler {
    private final AggregationService service; // Service holding aggregated weather records
    private final LamportClock clock;         // Lamport clock for logical timestamp tracking

    // Constructor initializes the handler with the service and clock
    public GetWeatherHandler(AggregationService service, LamportClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Allow only GET requests, reject others with 400 (Bad Request)
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Always attach Lamport clock in response headers
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(400, -1); // -1 → no body in response
            exchange.close();
            return;
        }

        // Parse Lamport timestamp from the request (0 if missing or invalid)
        long incoming = parseLamportHeader(exchange);
        if (incoming > 0) {
            // Merge the incoming Lamport timestamp with the local clock
            clock.receive(incoming);
        }

        // Fetch snapshot of all stored weather data in JSON form
        // If no data exists, result is an empty string
        String body = service.isEmpty() ? "" : service.snapshotJson();

        // Always include the Lamport timestamp in the response header
        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));

        if (body.isBlank()) {
            // No weather data → return 204 No Content
            exchange.sendResponseHeaders(204, -1);
        } else {
            // Weather data available → return 200 OK with JSON payload
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); // Write JSON response
        }

        // Finalize response
        exchange.close();
    }

    /**
     * Utility method to parse Lamport timestamp from request headers.
     * Returns 0 if the header is missing or invalid.
     */
    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return 0L; // If parsing fails, treat as no timestamp
        }
    }
}
