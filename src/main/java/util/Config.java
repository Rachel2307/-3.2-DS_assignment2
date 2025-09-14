package util;

// Central config class to avoid magic numbers or strings
public final class Config {
    public static final int DEFAULT_PORT = 4567;       // default server port
    public static final int EXPIRY_SECONDS = 30;      // sources expire after 30s
    public static final int HISTORY_CAP = 20;         // max per-source history (optional)
    public static final int HTTP_TIMEOUT_MS = 4000;   // HTTP request timeout in ms

    public static final String LAMPORT_HEADER = "X-Lamport"; // header for Lamport timestamp

    public static final String DATA_DIR = "state";    // directory to store state files
    public static final String STATE_FILE = "state.json"; // main state file name

    private Config() {} // prevent instantiation
}
