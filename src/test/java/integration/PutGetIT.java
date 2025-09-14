package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.*;

public class PutGetIT extends BaseIT {

    @Test
    void putThenGetReturns200AndBody() {
        var put = ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0);
        assertTrue(put.statusCode() == 201 || put.statusCode() == 200,
                "PUT should be 201 or 200, was " + put.statusCode());

        var get = ClientDrivers.getSnapshot(server.baseUrl(), 0);
        assertEquals(200, get.statusCode(), "GET after PUT should be 200");
        assertTrue(get.body().contains("IDS60901"));
    }

    @Test
    void secondPutIs200() {
        ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0);
        var second = ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0);
        assertEquals(200, second.statusCode(), "Second PUT should be 200");
    }
}
