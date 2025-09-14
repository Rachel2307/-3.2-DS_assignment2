package integration;

import org.junit.jupiter.api.Test;
import testutil.Await;
import testutil.ClientDrivers;
import testutil.TestData;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpiryIT extends BaseIT {

    @Test
    void afterTtlSnapshotReturns204() {
        ClientDrivers.putWeather(server.baseUrl(), TestData.minimalValid("IDS60901"), 0);
        // TTL configured to ~2s in ServerLauncher
        Await.until(Duration.ofSeconds(3), Duration.ofMillis(100),
                () -> ClientDrivers.getSnapshot(server.baseUrl(), 0).statusCode() == 204);
        var res = ClientDrivers.getSnapshot(server.baseUrl(), 0);
        assertEquals(204, res.statusCode(), "after TTL, snapshot should be 204");
    }

    @Test
    void afterTtl_getByIdReturns204() {
        ClientDrivers.putWeather(server.baseUrl(), TestData.minimalValid("IDS60901"), 0);
        Await.until(Duration.ofSeconds(3), Duration.ofMillis(100),
                () -> ClientDrivers.getById(server.baseUrl(), "IDS60901", 0).statusCode() == 204);
        var res = ClientDrivers.getById(server.baseUrl(), "IDS60901", 0);
        assertEquals(204, res.statusCode(), "after TTL, GET by id should be 204");
    }
}
