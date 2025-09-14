package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LamportIT extends BaseIT {

    @Test
    void lamportMonotonicity_put_get_put_get() {
        long l1 = ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), 0)
                .headers().firstValueAsLong("X-Lamport").orElse(0L);
        long l2 = ClientDrivers.getSnapshot(server.baseUrl(), l1)
                .headers().firstValueAsLong("X-Lamport").orElse(0L);
        long l3 = ClientDrivers.putWeather(server.baseUrl(), TestData.validFlat("IDS60901"), l2)
                .headers().firstValueAsLong("X-Lamport").orElse(0L);
        long l4 = ClientDrivers.getSnapshot(server.baseUrl(), l3)
                .headers().firstValueAsLong("X-Lamport").orElse(0L);

        assertTrue(l1 < l2 && l2 < l3 && l3 < l4,
                "Lamport should strictly increase across operations");
    }
}
