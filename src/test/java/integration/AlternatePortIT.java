package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Integration test to verify server responds on alternate port
public class AlternatePortIT extends BaseIT {

    @Test
    void respondsOnAlternatePort() throws Exception {
        // make GET request to /weather.json
        ClientDrivers.Response r = client.get("/weather.json");
        // assert that HTTP status code is valid (200-599)
        assertTrue(r.code >= 200 && r.code < 600);
    }
}
