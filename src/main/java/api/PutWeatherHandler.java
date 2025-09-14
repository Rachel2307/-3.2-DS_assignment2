package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler that processes PUT requests to store or update weather data.
 * Integrates Lamport clocks for distributed logical time synchronization.
 */
public final class PutWeatherHandler implements HttpHandler {
    private final AggregationService service; // Service responsible for ingesting/storing weather data
    private final LamportClock clock;         // Lamport clock instance to maintain logical ordering

    // Constructor to initialize service and Lamport clock
    public PutWeatherHandler(AggregationService service, LamportClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only allow PUT requests. If not PUT, return 400 (Bad Request).
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(400, -1); // -1 means no response body
            exchange.close();
            return;
        }

        // Lamport clock update: merge exactly once for inbound PUT request
        long incoming = parseLamportHeader(exchange);
        clock.receive(incoming); // Merge sender's Lamport timestamp with local clock

        // Read and trim the request body (JSON string representing weather data)
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();

        // If body is blank, respond with 204 (No Content)
        if (body.isBlank()) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        int status;
        try {
            // Ingest the weather data into the service
            // Returns 201 if it's the first time (created), otherwise 200 (updated)
            status = service.ingest(body, clock);
        } catch (IllegalArgumentException bad) {
            // If ingest fails, determine error code:
            // - If message contains "invalid" → 500 (Internal Server Error)
            // - Otherwise → 400 (Bad Request)
            int code = bad.getMessage().toLowerCase().contains("invalid") ? 500 : 400;
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
            return;
        }

        // If successful, attach Lamport clock header and return 200 or 201
        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    /**
     * Parses Lamport timestamp from the request header.
     * Returns 0 if the header is missing or invalid.
     */
    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try {
            return Long.parseLong(v.trim()); // Convert to long if valid
        } catch (Exception e) {
            return 0L; // Invalid format → ignore and return 0
        }
    }
}
