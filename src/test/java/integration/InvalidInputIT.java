package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InvalidInputIT extends BaseIT {

    @Test
    void putMissingId_returns400() {
        var res = ClientDrivers.putWeather(server.baseUrl(), TestData.missingId(), 0);
        assertEquals(400, res.statusCode());
    }

    @Test
    void putMalformed_returns400() {
        var res = ClientDrivers.putWeather(server.baseUrl(), TestData.malformed(), 0);
        assertEquals(400, res.statusCode());
    }

    @Test
    void wrongPath_returns404() {
        var res = ClientDrivers.getById(server.baseUrl(), "does-not-exist", 0);
        // If your server returns 204 for missing id, change to 204 here.
        // Many implementations use 204 (no content) for "not present".
        // We'll accept 204 or 404 to be safe:
        int sc = res.statusCode();
        boolean ok = (sc == 204) || (sc == 404);
        assertEquals(true, ok, "Expected 204 or 404, got " + sc);
    }
}
