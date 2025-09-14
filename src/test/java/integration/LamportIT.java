package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Integration test to verify Lamport ordering with interleaved PUT/GET
public class LamportIT extends BaseIT {

    @Test
    void interleavedPutGetRespectsOrder() throws Exception {
        // PUT Adelaide record
        ClientDrivers.Response p1 = client.putWeather(TestData.adelaideJson());
        assertTrueCode(p1.code, 201, 200, "First PUT should be 201/200");

        // GET should see Adelaide but not Sydney yet
        ClientDrivers.Response g1 = client.getAll();
        JsonAsserts.bodyContainsId(g1.body, "IDS60901");
        JsonAsserts.bodyNotContainsId(g1.body, "IDS60902");

        // PUT Sydney record
        ClientDrivers.Response p2 = client.putWeather(TestData.sydneyJson());
        // code can be 201 (new) or 200 (update)
        assertTrueCode(p2.code, 201, 200, "Second id PUT should be 201/200");

        // GET should now see both Adelaide and Sydney
        ClientDrivers.Response g2 = client.getAll();
        JsonAsserts.bodyContainsId(g2.body, "IDS60901");
        JsonAsserts.bodyContainsId(g2.body, "IDS60902");
        assertEquals(200, g2.code, "GET should return 200");
    }

    // helper to check response code against two allowed values
    private static void assertTrueCode(int actual, int a, int b, String msg) {
        if (actual != a && actual != b) {
            throw new AssertionError(msg + " but got " + actual);
        }
    }
}
