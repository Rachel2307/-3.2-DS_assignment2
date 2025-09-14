package integration;

import org.junit.jupiter.api.Test;
import testutil.ClientDrivers;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StartupIT extends BaseIT {

    @Test
    void serverStartsAndListens() {
        var res = ClientDrivers.getSnapshot(server.baseUrl(), 0);
        // fresh store => 204 No Content
        assertEquals(204, res.statusCode(), "fresh snapshot should be empty (204)");
    }
}
