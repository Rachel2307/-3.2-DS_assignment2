package util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Minimal periodic scheduler used to trigger the 30s expiry check.
 * Runs as a daemon so it won't block JVM shutdown.
 */
public final class ExpiryScheduler {
    private final Timer timer = new Timer("expiry", true);

    public void everyMillis(long periodMillis, Runnable task) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { task.run(); }
        }, periodMillis, periodMillis);
    }

    public void stop() { timer.cancel(); }
}
