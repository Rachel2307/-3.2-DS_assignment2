package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlternatePortIT extends BaseIT {

    @Test
    void respondsOnAlternatePort() throws Exception {
        ClientDrivers.Response r = client.get("/weather.json");
        assertTrue(r.code >= 200 && r.code < 600);
    }
}
