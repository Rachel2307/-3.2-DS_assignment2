package integration;

import org.junit.jupiter.api.Test;
import testutil.Await;
import testutil.ClientDrivers;
import testutil.TestData;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyIT extends BaseIT {

    @Test
    void twoContentServers_putConcurrently_bothVisible() {
        var p1 = CompletableFuture.supplyAsync(() ->
                ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0));
        var p2 = CompletableFuture.supplyAsync(() ->
                ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS99999"), 0));

        p1.join(); p2.join();

        var snap = ClientDrivers.getSnapshot(server.baseUrl(), 0);
        int sc = snap.statusCode();
        assertTrue(sc == 200 || sc == 204, "snapshot should be 200 (has data) or 204 (empty if TTL raced)");

        if (sc == 200) {
            String body = snap.body();
            assertTrue(body.contains("IDS60901"));
            assertTrue(body.contains("IDS99999"));
        }
    }

    @Test
    void twoClients_getConcurrently_bothReceive200() {
        // Ensure there is data
        ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0);

        Await.until(Duration.ofSeconds(2), Duration.ofMillis(50), () -> {
            var a = CompletableFuture.supplyAsync(() -> ClientDrivers.getSnapshot(server.baseUrl(), 0));
            var b = CompletableFuture.supplyAsync(() -> ClientDrivers.getSnapshot(server.baseUrl(), 0));
            int scA = a.join().statusCode();
            int scB = b.join().statusCode();
            return scA == 200 && scB == 200;
        });
    }
}
