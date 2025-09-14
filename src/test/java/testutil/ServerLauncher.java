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

public final class ServerLauncher {
    private final int port;
    private final List<String> jvmProps = new ArrayList<>();
    private Process proc; // non-null only if we launched it

    public ServerLauncher(int port) { this.port = port; }

    public ServerLauncher withJvmProp(String k, String v) {
        jvmProps.add("-D" + k + "=" + v);
        return this;
    }

    public void start() throws Exception {
        // If something is already listening, just use it.
        if (responds(port)) return;

        String cp = System.getProperty("java.class.path");
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.addAll(jvmProps);
        cmd.add("-cp");
        cmd.add(cp);
        // ✅ Correct main class:
        cmd.add("app.AggregationServer");
        // ✅ Pass the port arg (your server reads it):
        cmd.add(Integer.toString(port));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        proc = pb.start();

        // Stream output only when -Dtests.verbose=true
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

        // Wait for /weather.json to respond (200–599 or 204 is fine)
        untilTrue(() -> responds(port), Duration.ofSeconds(10), Duration.ofMillis(150));
    }

    public void stop() {
        if (proc != null) {
            proc.destroy();
            try { proc.waitFor(); } catch (InterruptedException ignored) {}
            proc = null;
        }
    }

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
