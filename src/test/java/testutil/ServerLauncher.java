package testutil;

import api.Routes;
import com.sun.net.httpserver.HttpServer;
import core.AggregationService;
import core.LamportClock;
import store.StateStore;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Starts your app in-process on a random free port, short TTL (2s).
 * Detects any of these overloads:
 *  - Routes.start(HttpServer, AggregationService, LamportClock, int)
 *  - Routes.start(HttpServer, AggregationService, LamportClock)
 *  - Routes.start(int, AggregationService, LamportClock)
 */
public final class ServerLauncher {
    private HttpServer server;
    private String baseUrl;
    private final Path tmpDir;

    public ServerLauncher() {
        try {
            this.tmpDir = Files.createTempDirectory("asm2-tests");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            // backing store (file path under temp dir)
            Path storeFile = tmpDir.resolve("state.json");
            StateStore store = new StateStore(storeFile);

            // LamportClock is now required by AggregationService
            LamportClock lc = new LamportClock("it-tests");

            // NOTE: Your AggregationService constructor now takes (StateStore, LamportClock)
            AggregationService svc = new AggregationService(store, lc);

            // bind on random port
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            int port = server.getAddress().getPort();

            // reflect Routes.start(...)
            boolean started = false;
            for (Method m : Routes.class.getDeclaredMethods()) {
                if (!m.getName().equals("start")) continue;
                Class<?>[] p = m.getParameterTypes();
                try {
                    if (p.length == 4 &&
                            p[0] == HttpServer.class &&
                            p[1] == AggregationService.class &&
                            p[2] == LamportClock.class &&
                            p[3] == int.class) {
                        m.invoke(null, server, svc, lc, 2);
                        started = true;
                        break;
                    }
                    if (p.length == 3 &&
                            p[0] == HttpServer.class &&
                            p[1] == AggregationService.class &&
                            p[2] == LamportClock.class) {
                        m.invoke(null, server, svc, lc);
                        started = true;
                        break;
                    }
                    if (p.length == 3 &&
                            p[0] == int.class &&
                            p[1] == AggregationService.class &&
                            p[2] == LamportClock.class) {
                        m.invoke(null, port, svc, lc);
                        started = true;
                        break;
                    }
                } catch (Exception ignore) {
                    // try next overload
                }
            }
            if (!started) {
                throw new IllegalStateException("""
                    Could not locate a compatible Routes.start(...) method.
                    Expected one of:
                     - start(HttpServer, AggregationService, LamportClock, int)
                     - start(HttpServer, AggregationService, LamportClock)
                     - start(int, AggregationService, LamportClock)
                    """);
            }

            server.start();
            this.baseUrl = "http://localhost:" + port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public void restart() {
        stop();
        start();
    }

    public String baseUrl() {
        return baseUrl;
    }
}
