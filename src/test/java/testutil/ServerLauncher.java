package testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static testutil.Await.untilTrue;

// Helper to launch a server process for integration tests
public final class ServerLauncher {
    private final int port; // server port to launch
    private final List<String> jvmProps = new ArrayList<>(); // JVM system properties
    private Process proc; // process handle if we started the server

    public ServerLauncher(int port) { this.port = port; }

    // add JVM property (e.g., -Dkey=value)
    public ServerLauncher withJvmProp(String k, String v) {
        jvmProps.add("-D" + k + "=" + v);
        return this;
    }

    // start server (or reuse if already listening)
    public void start() throws Exception {
        if (responds(port)) return; // server already running

        String cp = System.getProperty("java.class.path"); // classpath
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java"); // java binary
        cmd.addAll(jvmProps); // optional JVM properties
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add("app.AggregationServer"); // main class
        cmd.add(Integer.toString(port)); // pass port argument

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // merge stdout/stderr
        proc = pb.start();

        // log server output if tests.verbose=true
        new Thread(() -> {
            try (var br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (Boolean.getBoolean("tests.verbose")) {
                        System.out.println("[server] " + line);
                    }
                }
            } catch (IOException ignored) {}
        }, "server-stdout").start();

        // wait until /weather.json responds
        untilTrue(() -> responds(port), Duration.ofSeconds(10), Duration.ofMillis(150));
    }

    // stop server process if started
    public void stop() {
        if (proc != null) {
            proc.destroy();
            try { proc.waitFor(); } catch (InterruptedException ignored) {}
            proc = null;
        }
    }

    // check if server responds at /weather.json
    private static boolean responds(int port) {
        try {
            HttpClient c = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/weather.json"))
                    .timeout(Duration.ofMillis(600))
                    .GET()
                    .build();
            HttpResponse<String> r = c.send(req, HttpResponse.BodyHandlers.ofString());
            return r.statusCode() >= 200 && r.statusCode() < 600 || r.statusCode() == 204;
        } catch (Exception e) {
            return false;
        }
    }
}