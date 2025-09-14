package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;
import testutil.JsonAsserts;
import testutil.ServerLauncher;
import testutil.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersistenceIT extends BaseIT {

    @Test
    void survivesRestart_stateJsonRestored() throws Exception {
        // Put a record
        var put = client.putWeather(TestData.adelaideJson());
        // accept 200 or 201
        assertEquals(true, put.code == 200 || put.code == 201);

        // Stop server (simulate crash)
        server.stop();

        // Restart on same port so it reuses same state dir
        server = new ServerLauncher(port)
                .withJvmProp("asm.port", Integer.toString(port));
        server.start();
        client = new ClientDrivers(port);

        // Verify data is still there
        var get = client.getAll();
        assertEquals(200, get.code);
        JsonAsserts.bodyContainsId(get.body, "IDS60901");
    }
}
