package api;

import com.sun.net.httpserver.HttpServer;
import core.AggregationService;
import core.LamportClock;
import util.Config;

import java.io.IOException;

/**
 * Wires HTTP paths (routes) to their respective handlers.
 * This class is responsible for registering endpoints on the server.
 *
 * Note: No handler classes are directly public here; they are invoked internally.
 */
public final class Routes {
    // Private constructor → prevents instantiation of utility class
    private Routes() {}

    /**
     * Register all routes on the given HttpServer instance.
     *
     * Supported paths:
     *  - PUT /weather.json  -> handled by PutWeatherHandler (store/update weather data)
     *  - GET /weather.json  -> handled by GetWeatherHandler (return all records)
     *  - GET /weather/{id}  -> handled by GetWeatherByIdHandler (return single record by ID)
     */
    public static void register(HttpServer server, AggregationService service, LamportClock clock) throws IOException {
        // Create combined context for /weather.json
        // This single path supports both PUT and GET methods
        server.createContext("/weather.json", exchange -> {
            try {
                String method = exchange.getRequestMethod();
                if ("PUT".equalsIgnoreCase(method)) {
                    // Forward PUT request to PutWeatherHandler
                    new PutWeatherHandler(service, clock).handle(exchange);
                } else if ("GET".equalsIgnoreCase(method)) {
                    // Forward GET request to GetWeatherHandler
                    new GetWeatherHandler(service, clock).handle(exchange);
                } else {
                    // Any method other than GET or PUT → return 400 (Bad Request)
                    exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                }
            } catch (Exception e) {
                // If handler throws an exception → return 500 (Internal Server Error)
                try {
                    exchange.getResponseHeaders().set(Config.LAMPORT_HEADER, String.valueOf(clock.now()));
                    exchange.sendResponseHeaders(500, -1);
                } finally {
                    exchange.close();
                }
            }
        });

        // Create prefix context for /weather/{id}
        // HttpServer performs prefix matching (e.g. /weather/IDS60901 will match /weather)
        // The actual station ID is parsed later by GetWeatherByIdHandler.
        server.createContext("/weather", exchange -> {
            try {
                new GetWeatherByIdHandler(service, clock).handle(exchange);
            } catch (Exception e) {
                // If handler throws an exception → return 500 (Internal Server Error)
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
