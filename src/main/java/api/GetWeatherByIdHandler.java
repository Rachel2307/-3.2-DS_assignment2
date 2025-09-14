package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class GetWeatherByIdHandler implements HttpHandler {
    private final AggregationService service;
    private final LamportClock clock;

    public GetWeatherByIdHandler(AggregationService service, LamportClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        long incoming = parseLamportHeader(exchange); // 0 if absent/invalid
        if (incoming > 0) {
            clock.receive(incoming);
        }

        String id = extractId(exchange.getRequestURI());
        String body = (id == null) ? null : service.singleJson(id);

        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
        if (id == null) {
            exchange.sendResponseHeaders(400, -1);
        } else if (body == null || body.isBlank()) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String extractId(URI uri) {
        String path = uri.getPath(); // e.g. /weather/IDS60901
        if (path == null) return null;
        String[] parts = path.split("/");
        return (parts.length >= 3) ? parts[2] : null;
    }

    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return 0L; }
    }
}
