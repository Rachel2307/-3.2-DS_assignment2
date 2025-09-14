package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import java.time.Duration;

import static testutil.Await.untilTrue;

public class ExpiryIT extends BaseIT {

    @Test
    void recordsExpireAfter30Seconds() throws Exception {
        client.putWeather(TestData.adelaideJson());
        var g1 = client.getAll();
        JsonAsserts.bodyContainsId(g1.body, "IDS60901");

        // Wait up to 40s for expiry
        untilTrue(() -> {
            try {
                var g = client.getAll();
                return g.body == null || !g.body.contains("IDS60901");
            } catch (Exception ignored) { return false; }
        }, Duration.ofSeconds(40), Duration.ofSeconds(1));
    }
}
