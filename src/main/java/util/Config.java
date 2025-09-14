package util;

/** Central config to avoid magic numbers/strings. */
public final class Config {
    public static final int DEFAULT_PORT = 4567;
    public static final int EXPIRY_SECONDS = 30;          // assignment requires 30s
    public static final int HISTORY_CAP = 20;             // (if you later add per-source history)
    public static final int HTTP_TIMEOUT_MS = 4000;

    public static final String LAMPORT_HEADER = "X-Lamport";

    public static final String DATA_DIR = "state";
    public static final String STATE_FILE = "state.json";

    private Config() {}
}
