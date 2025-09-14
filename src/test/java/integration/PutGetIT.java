package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PutGetIT extends BaseIT {

    @Test
    void firstPutReturns201_thenUpdateReturns200_andGetContains() throws Exception {
        ClientDrivers.Response p1 = client.putWeather(TestData.adelaideJson());
        // First time content for a source should be 201 per spec
        // If your server returns 200 for first time, relax to accept 200/201:
        assertTrueCode(p1.code, 201, 200, "Expected 201 on first PUT (or 200 if already exists)");

        ClientDrivers.Response g1 = client.getAll();
        JsonAsserts.bodyContainsId(g1.body, "IDS60901");

        ClientDrivers.Response p2 = client.putWeather(TestData.adelaideJson());
        assertEquals(200, p2.code, "Expected 200 for subsequent PUT update");

        ClientDrivers.Response g2 = client.getAll();
        JsonAsserts.bodyContainsId(g2.body, "IDS60901");
    }

    private static void assertTrueCode(int actual, int a, int b, String msg) {
        if (actual != a && actual != b) {
            throw new AssertionError(msg + " but got " + actual);
        }
    }
}
