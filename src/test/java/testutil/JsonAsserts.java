package testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonAsserts {
    private JsonAsserts(){}

    public static void contains(String json, String... snippets) {
        for (String s : snippets) {
            assertTrue(json.contains(s), () -> "JSON should contain snippet: " + s + "\nJSON: " + json);
        }
    }
}
