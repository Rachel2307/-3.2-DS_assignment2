package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import java.time.Duration;

import static testutil.Await.untilTrue;

// Integration test to verify that records expire after configured time
public class ExpiryIT extends BaseIT {

    @Test
    void recordsExpireAfter30Seconds() throws Exception {
        client.putWeather(TestData.adelaideJson()); // add Adelaide record
        var g1 = client.getAll();
        JsonAsserts.bodyContainsId(g1.body, "IDS60901"); // check it is present initially

        // Wait up to 40s for the record to expire (poll every 1s)
        untilTrue(() -> {
            try {
                var g = client.getAll();
                return g.body == null || !g.body.contains("IDS60901"); // return true if expired
            } catch (Exception ignored) { return false; }
        }, Duration.ofSeconds(40), Duration.ofSeconds(1));
    }
}
