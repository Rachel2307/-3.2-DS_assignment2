package app;

import api.Routes;
import com.sun.net.httpserver.HttpServer;
import core.AggregationService;
import core.LamportClock;
import store.StateStore;
import util.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aggregation server bootstrap:
 * - Starts HttpServer on provided port (default 4567)
 * - Wires routes via Routes.register(...)
 * - Sets up a lightweight scheduler that calls service.pruneExpired(...) every second
 *   (No Lamport tick for expiry; logs use current clock.now())
 * - Persists state via StateStore
 */
public final class AggregationServer {

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0) ? parsePort(args[0]) : 4567;

        // Lamport clock for the server
        LamportClock clock = new LamportClock("server");

        // Persist state under ./state (change the folder name if you like)
        StateStore store = new StateStore(Paths.get("state"));

        // Core service (writer thread started inside)
        AggregationService service = new AggregationService(store, clock);

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Routes.register(server, service, clock);
        server.start();

        System.out.println("HTTP server listening on " + port);
        System.out.printf("AggregationServer started on port %d  (Lamport=%d, expiry=%ds)%n",
                port, clock.now(), Config.EXPIRY_SECONDS);

        // In-process scheduler for expiry: run every 1 second after 1s initial delay
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "expiry-scheduler");
            t.setDaemon(true);
            return t;
        });
        ses.scheduleAtFixedRate(() -> {
            try {
                service.pruneExpired(Config.EXPIRY_SECONDS);
            } catch (Throwable t) {
                // Don't crash the scheduler; print once
                System.err.println("[expiry] error: " + t.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop(0);
            } catch (Throwable ignored) {}
            try {
                ses.shutdownNow();
            } catch (Throwable ignored) {}
        }, "shutdown-hook"));
    }

    private static int parsePort(String s) {
        Objects.requireNonNull(s, "port");
        try {
            int p = Integer.parseInt(s.trim());
            if (p <= 0 || p > 65535) throw new IllegalArgumentException("invalid port range");
            return p;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("port must be an integer: " + s, nfe);
        }
    }
}
