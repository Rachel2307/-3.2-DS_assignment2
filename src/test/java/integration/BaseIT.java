package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import testutil.ClientDrivers;
import testutil.ServerLauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseIT {
    protected int port;
    protected ServerLauncher server;
    protected ClientDrivers client;
    protected Path stateDir;

    @BeforeEach
    void boot() throws Exception {
        // Use an ephemeral port & pass it to the server
        port = pickFreePort();
        stateDir = Path.of("state");
        // Clean persisted state between tests
        if (Files.exists(stateDir)) {
            Files.walk(stateDir)
                    .sorted((a,b)->b.compareTo(a))
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }

        server = new ServerLauncher(port);
        server.start();
        client = new ClientDrivers(port);
    }

    @AfterEach
    void shutdown() {
        if (server != null) server.stop();
    }

    private static int pickFreePort() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
