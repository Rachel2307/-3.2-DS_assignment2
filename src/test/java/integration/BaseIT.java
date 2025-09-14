package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import testutil.ClientDrivers;
import testutil.ServerLauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Base class for integration tests, sets up server & client
public abstract class BaseIT {
    protected int port;               // port for server
    protected ServerLauncher server;  // server instance
    protected ClientDrivers client;   // HTTP client
    protected Path stateDir;          // directory for persisted state

    @BeforeEach
    void boot() throws Exception {
        // pick a free ephemeral port
        port = pickFreePort();
        stateDir = Path.of("state");
        // clean state directory before each test
        if (Files.exists(stateDir)) {
            Files.walk(stateDir)
                    .sorted((a,b)->b.compareTo(a)) // delete files before directories
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }

        server = new ServerLauncher(port); // launch server
        server.start();
        client = new ClientDrivers(port);  // create client
    }

    @AfterEach
    void shutdown() {
        if (server != null) server.stop(); // stop server after test
    }

    // pick a free ephemeral port for testing
    private static int pickFreePort() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
