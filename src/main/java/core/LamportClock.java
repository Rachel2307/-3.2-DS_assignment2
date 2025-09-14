package core;

import java.util.concurrent.atomic.AtomicLong;

// Lamport logical clock for ordering events across nodes
// Provides send(), receive(), and read-only now()
public final class LamportClock {
    private final String id;                   // unique ID for this clock
    private final AtomicLong time = new AtomicLong(0); // current Lamport time

    public LamportClock(String id) {
        this.id = id; // set clock ID
    }

    // read current value without increment
    public long now() {
        return time.get();
    }

    // outbound event: increment and return value to send
    public long send() {
        return time.incrementAndGet();
    }

    // inbound event: merge remote value, advance by +1
    public long receive(long remote) {
        if (remote < 0) remote = 0;
        while (true) {
            long cur = time.get();
            long next = Math.max(cur, remote) + 1;
            if (time.compareAndSet(cur, next)) return next;
        }
    }

    // legacy method: no-op for tickProcess to avoid extra increment
    public long tickProcess() {
        return now();
    }

    // legacy shim for old tickSend() calls
    public long tickSend() {
        return send();
    }

    // legacy shim for old onReceive() calls
    public void onReceive(long remote) {
        receive(remote);
    }

    // legacy name for read-only peek
    public long peek() {
        return now();
    }

    // get clock ID
    public String id() {
        return id;
    }
}
