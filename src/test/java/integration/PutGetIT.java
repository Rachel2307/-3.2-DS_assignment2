package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Integration test to verify PUT/GET behavior for a single source
public class PutGetIT extends BaseIT {

    @Test
    void firstPutReturns201_thenUpdateReturns200_andGetContains() throws Exception {
        // First PUT for Adelaide record
        ClientDrivers.Response p1 = client.putWeather(TestData.adelaideJson());
        // first-time PUT should be 201, but accept 200 if already exists
        assertTrueCode(p1.code, 201, 200, "Expected 201 on first PUT (or 200 if already exists)");

        // GET should include Adelaide
        ClientDrivers.Response g1 = client.getAll();
        JsonAsserts.bodyContainsId(g1.body, "IDS60901");

        // Second PUT (update) for same source
        ClientDrivers.Response p2 = client.putWeather(TestData.adelaideJson());
        assertEquals(200, p2.code, "Expected 200 for subsequent PUT update");

        // GET should still include Adelaide
        ClientDrivers.Response g2 = client.getAll();
        JsonAsserts.bodyContainsId(g2.body, "IDS60901");
    }

    // helper to check response code against two allowed values
    private static void assertTrueCode(int actual, int a, int b, String msg) {
        if (actual != a && actual != b) {
            throw new AssertionError(msg + " but got " + actual);
        }
    }
}
