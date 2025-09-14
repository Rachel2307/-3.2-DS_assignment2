package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartupIT extends BaseIT {

    @Test
    void serverRespondsOnStartup() throws Exception {
        ClientDrivers.Response r = client.get("/weather.json");
        assertTrue(r.code >= 200 && r.code < 600, "Server should respond to GET /weather.json");
    }
}
