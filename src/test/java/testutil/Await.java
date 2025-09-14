package testutil;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

// Small helper for polling until a condition becomes true
public final class Await {
    private Await() {} // prevent instantiation

    // repeatedly check cond until it returns true or timeout is reached
    public static void untilTrue(BooleanSupplier cond, Duration timeout, Duration poll) {
        long deadline = System.nanoTime() + timeout.toNanos(); // compute end time
        while (System.nanoTime() < deadline) {
            if (cond.getAsBoolean()) return; // condition met
            try {
                Thread.sleep(poll.toMillis()); // wait before polling again
            } catch (InterruptedException ignored) {}
        }
        fail("Condition not met within " + timeout); // timeout reached
    }
}
