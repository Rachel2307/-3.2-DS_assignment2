package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyIT extends BaseIT {

    @Test
    void concurrentPuts_thenSequentialRepair_resultsInBothPresent() throws Exception {
        AtomicInteger codesOk = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                var r = client.putWeather(TestData.adelaideJson());
                if (r.code == 200 || r.code == 201) codesOk.incrementAndGet();
            } catch (Exception ignored) {}
            latch.countDown();
        });
        Thread t2 = new Thread(() -> {
            try {
                var r = client.putWeather(TestData.sydneyJson());
                if (r.code == 200 || r.code == 201) codesOk.incrementAndGet();
            } catch (Exception ignored) {}
            latch.countDown();
        });

        t1.start(); t2.start();
        latch.await();

        // Both requests should have been OK (no 5xx)
        assertTrue(codesOk.get() == 2, "Both concurrent PUTs should return 200/201");

        // After the race, at least one id should be visible (order-agnostic)
        ClientDrivers.Response gAfterRace = client.getAll();
        JsonAsserts.bodyContainsAnyId(gAfterRace.body, "IDS60901", "IDS60902");

        // If one is missing, PUT it sequentially and verify both present
        boolean hasAdl = gAfterRace.body != null && gAfterRace.body.contains("IDS60901");
        boolean hasSyd = gAfterRace.body != null && gAfterRace.body.contains("IDS60902");

        if (!hasAdl) client.putWeather(TestData.adelaideJson());
        if (!hasSyd) client.putWeather(TestData.sydneyJson());

        ClientDrivers.Response gFinal = client.getAll();
        JsonAsserts.bodyContainsId(gFinal.body, "IDS60901");
        JsonAsserts.bodyContainsId(gFinal.body, "IDS60902");
        assertTrue(gFinal.code == 200 || gFinal.code == 204 || gFinal.code == 201);
    }
}
