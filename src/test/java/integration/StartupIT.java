package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Integration test to verify server responds immediately on startup
public class StartupIT extends BaseIT {

    @Test
    void serverRespondsOnStartup() throws Exception {
        // make GET request to /weather.json
        ClientDrivers.Response r = client.get("/weather.json");
        // assert response code is in valid HTTP range
        assertTrue(r.code >= 200 && r.code < 600, "Server should respond to GET /weather.json");
    }
}
