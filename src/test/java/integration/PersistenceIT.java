package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.ServerLauncher;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Integration test to verify server persists state across restarts
public class PersistenceIT extends BaseIT {

    @Test
    void survivesRestart_stateJsonRestored() throws Exception {
        // PUT a record for Adelaide
        var put = client.putWeather(TestData.adelaideJson());
        assertEquals(true, put.code == 200 || put.code == 201); // accept 200 or 201

        // Stop server to simulate crash
        server.stop();

        // Restart server on same port to reuse state directory
        server = new ServerLauncher(port)
                .withJvmProp("asm.port", Integer.toString(port));
        server.start();
        client = new ClientDrivers(port);

        // Verify the Adelaide record is still present
        var get = client.getAll();
        assertEquals(200, get.code);
        JsonAsserts.bodyContainsId(get.body, "IDS60901");
    }
}
