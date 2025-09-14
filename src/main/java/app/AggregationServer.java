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
 * This is the main AggregationServer program.
 * - It starts an HTTP server on a given port (default is 4567).
 * - It sets up the routes with Routes.register().
 * - Runs a small scheduler to remove expired data every second.
 * - Saves the state to disk using StateStore.
 */
public final class AggregationServer {

    public static void main(String[] args) throws Exception {
        // If a port number is passed in args, use it. Otherwise use default 4567.
        int port = (args.length > 0) ? parsePort(args[0]) : 4567;

        // Create a Lamport clock for this server
        LamportClock clock = new LamportClock("server");

        // Store data in the ./state folder
        StateStore store = new StateStore(Paths.get("state"));

        // Create the core service (this also starts a writer thread)
        AggregationService service = new AggregationService(store, clock);

        // Start the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Routes.register(server, service, clock);
        server.start();

        System.out.println("HTTP server listening on " + port);
        System.out.printf("AggregationServer started on port %d (Lamport=%d, expiry=%ds)%n",
                port, clock.now(), Config.EXPIRY_SECONDS);

        // Scheduler that checks every second and removes expired data
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "expiry-scheduler");
            t.setDaemon(true);
            return t;
        });
        ses.scheduleAtFixedRate(() -> {
            try {
                service.pruneExpired(Config.EXPIRY_SECONDS);
            } catch (Throwable t) {
                // Just print error, don’t crash
                System.err.println("[expiry] error: " + t.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        // Graceful shutdown: stop server and scheduler when program ends
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop(0);
            } catch (Throwable ignored) {}
            try {
                ses.shutdownNow();
            } catch (Throwable ignored) {}
        }, "shutdown-hook"));
    }

    // Helper to parse port from command line
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
