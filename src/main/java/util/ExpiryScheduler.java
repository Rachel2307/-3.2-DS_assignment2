package util;

import java.util.Timer;
import java.util.TimerTask;

// Minimal scheduler to run periodic tasks (like expiry checks)
public final class ExpiryScheduler {
    private final Timer timer = new Timer("expiry", true); // daemon timer thread

    // run the given task every periodMillis milliseconds
    public void everyMillis(long periodMillis, Runnable task) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { task.run(); } // call the task
        }, periodMillis, periodMillis);
    }

    // stop the scheduler
    public void stop() { timer.cancel(); }
}
