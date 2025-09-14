package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InvalidInputIT extends BaseIT {

    @Test
    void postReturns400() throws Exception {
        ClientDrivers.Response r = client.postToWeather();
        assertEquals(400, r.code, "POST should yield 400 (per assignment simplification)");
    }

    @Test
    void emptyPutReturns204() throws Exception {
        ClientDrivers.Response r = client.putRaw(TestData.emptyBody(), "application/json");
        assertEquals(204, r.code, "Empty body PUT should return 204");
    }

    @Test
    void malformedJsonReturns500() throws Exception {
        ClientDrivers.Response r = client.putRaw(TestData.malformedJson(), "application/json");
        assertEquals(500, r.code, "Malformed JSON should return 500");
    }
}
