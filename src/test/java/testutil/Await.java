package testutil;

import java.time.Duration;

public final class Await {
    private Await() {}

    public static void until(Duration timeout, Duration poll, Condition cond) {
        long deadline = System.nanoTime() + timeout.toNanos();
        Throwable last = null;
        while (System.nanoTime() < deadline) {
            try {
                if (cond.ok()) return;
            } catch (Throwable t) {
                last = t;
            }
            try { Thread.sleep(poll.toMillis()); } catch (InterruptedException ignored) {}
        }
        if (last instanceof AssertionError ae) throw ae;
        throw new AssertionError("Condition not met within " + timeout);
    }

    @FunctionalInterface
    public interface Condition { boolean ok(); }
}
