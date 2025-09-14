package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler that processes GET requests to retrieve weather data by station ID.
 * Uses Lamport clocks for logical time synchronization between distributed services.
 */
public final class GetWeatherByIdHandler implements HttpHandler {
    private final AggregationService service; // Service to fetch aggregated weather data
    private final LamportClock clock;         // Lamport clock instance for logical time

    public GetWeatherByIdHandler(AggregationService service, LamportClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Only allow GET requests. If not GET, return 400 (Bad Request).
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(400, -1); // no response body
            exchange.close();
            return;
        }

        // Parse Lamport timestamp from request header (0 if absent or invalid).
        long incoming = parseLamportHeader(exchange);
        if (incoming > 0) {
            clock.receive(incoming); // Update Lamport clock if valid timestamp received.
        }

        // Extract station ID from request URI (e.g. /weather/IDS60901).
        String id = extractId(exchange.getRequestURI());
        // Fetch weather data as JSON from the aggregation service (null if not found).
        String body = (id == null) ? null : service.singleJson(id);

        // Always attach Lamport timestamp in response header.
        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));

        // Handle different cases based on ID and response body.
        if (id == null) {
            // No ID provided → 400 Bad Request
            exchange.sendResponseHeaders(400, -1);
        } else if (body == null || body.isBlank()) {
            // ID valid but no data found → 204 No Content
            exchange.sendResponseHeaders(204, -1);
        } else {
            // Found data → 200 OK with JSON response
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }

        // Close the exchange to finalize the response.
        exchange.close();
    }

    /**
     * Extracts the weather station ID from the request URI path.
     * Example: /weather/IDS60901 → IDS60901
     */
    private static String extractId(URI uri) {
        String path = uri.getPath(); // Get full path (e.g. /weather/IDS60901)
        if (path == null) return null;
        String[] parts = path.split("/");
        return (parts.length >= 3) ? parts[2] : null; // Return ID if exists
    }

    /**
     * Parses Lamport timestamp from request headers.
     * Returns 0 if header is missing or cannot be parsed.
     */
    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return 0L; // Invalid number → return 0
        }
    }
}
