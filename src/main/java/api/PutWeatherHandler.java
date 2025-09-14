package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PutWeatherHandler implements HttpHandler {
    private final AggregationService service;
    private final LamportClock clock;

    public PutWeatherHandler(AggregationService service, LamportClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        // Lamport: merge exactly once for inbound PUT
        long incoming = parseLamportHeader(exchange);
        clock.receive(incoming);

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();

        if (body.isBlank()) {
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        int status;
        try {
            status = service.ingest(body, clock); // 201 if first create, else 200
        } catch (IllegalArgumentException bad) {
            int code = bad.getMessage().toLowerCase().contains("invalid") ? 500 : 400;
            exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return 0L; }
    }
}
