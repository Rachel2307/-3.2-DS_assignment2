package testutil;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

public final class Await {
    private Await() {}

    public static void untilTrue(BooleanSupplier cond, Duration timeout, Duration poll) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (cond.getAsBoolean()) return;
            try {
                Thread.sleep(poll.toMillis());
            } catch (InterruptedException ignored) {}
        }
        fail("Condition not met within " + timeout);
    }
}
