package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.ServerLauncher;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlternatePortIT {

    @Test
    void serverStartsOnCustomPort_andAcceptsRequests() {
        ServerLauncher s = new ServerLauncher();
        s.start(); // random port; this test only checks the API works on a different instance
        try {
            var put = ClientDrivers.putWeather(s.baseUrl(), TestData.minimalValid("IDS60901"), 0);
            assertTrue(put.statusCode() == 201 || put.statusCode() == 200,
                    "PUT should be 201 or 200, was " + put.statusCode());
        } finally {
            s.stop();
        }
    }
}
