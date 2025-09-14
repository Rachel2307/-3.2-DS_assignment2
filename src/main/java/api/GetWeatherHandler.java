package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class GetWeatherHandler implements HttpHandler {
    private final AggregationService service;
    private final LamportClock clock;

    public GetWeatherHandler(AggregationService service, LamportClock clock) {
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

        // Only merge Lamport if the client actually sent one
        long incoming = parseLamportHeader(exchange);
        if (incoming > 0) {
            clock.receive(incoming);
        }

        String body = service.isEmpty() ? "" : service.snapshotJson();

        exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
        if (body.isBlank()) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static long parseLamportHeader(HttpExchange ex) {
        String v = ex.getRequestHeaders().getFirst(Config.LAMPORT_HEADER);
        if (v == null) return 0L;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return 0L; }
    }
}
