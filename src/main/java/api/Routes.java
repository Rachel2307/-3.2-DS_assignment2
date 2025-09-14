package api;

import com.sun.net.httpserver.HttpServer;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;

/**
 * Wires HTTP paths to handlers. No public handler classes live here.
 */
public final class Routes {
    private Routes() {}

    /**
     * Register all routes on the given server.
     *
     * Paths:
     *  - PUT /weather.json     -> PutWeatherHandler
     *  - GET /weather.json     -> GetWeatherHandler (all records)
     *  - GET /weather/{id}     -> GetWeatherByIdHandler (single record)
     */
    public static void register(HttpServer server, AggregationService service, LamportClock clock) throws IOException {
        // Combined context for /weather.json (supports both GET and PUT)
        server.createContext("/weather.json", exchange -> {
            try {
                String method = exchange.getRequestMethod();
                if ("PUT".equalsIgnoreCase(method)) {
                    new PutWeatherHandler(service, clock).handle(exchange);
                } else if ("GET".equalsIgnoreCase(method)) {
                    new GetWeatherHandler(service, clock).handle(exchange);
                } else {
                    // As per spec: anything other than GET/PUT -> 400
                    exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                }
            } catch (Exception e) {
                try {
                    exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
                    exchange.sendResponseHeaders(500, -1);
                } finally {
                    exchange.close();
                }
            }
        });

        // Prefix context for /weather/{id}
        // HttpServer matches by prefix; handler parses the id segment itself.
        server.createContext("/weather", exchange -> {
            try {
                new GetWeatherByIdHandler(service, clock).handle(exchange);
            } catch (Exception e) {
                try {
                    exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
                    exchange.sendResponseHeaders(500, -1);
                } finally {
                    exchange.close();
                }
            }
        });
    }
}
