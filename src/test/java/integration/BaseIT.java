package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import testutil.ServerLauncher;

public abstract class BaseIT {
    protected ServerLauncher server;

    @BeforeEach
    void setUp() {
        server = new ServerLauncher();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }
}
