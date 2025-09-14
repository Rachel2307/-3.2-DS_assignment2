package testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonAsserts {
    private JsonAsserts() {}

    public static void bodyContainsId(String body, String id) {
        assertTrue(body != null && body.contains("\"id\"") && body.contains(id),
                "Response body should contain id=" + id + " but was:\n" + body);
    }

    public static void bodyNotContainsId(String body, String id) {
        assertTrue(body == null || !body.contains(id),
                "Response body should NOT contain id=" + id + " but was:\n" + body);
    }

    public static void bodyContainsAnyId(String body, String... ids) {
        boolean found = false;
        if (body != null) {
            for (String id : ids) {
                if (body.contains(id)) { found = true; break; }
            }
        }
        assertTrue(found, "Response body should contain one of " + String.join(", ", ids) + " but was:\n" + body);
    }
}
