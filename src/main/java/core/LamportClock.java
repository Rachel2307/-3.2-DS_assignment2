package core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lamport logical clock with a minimal, safe surface:
 * - send(): increment once for an outbound message; returns value to send
 * - receive(m): merge for an inbound message; returns new local value
 * - now(): read current without ticking
 *
 * Backward-compat shims:
 * - tickSend() -> send()
 * - onReceive(m) -> receive(m)
 * - tickProcess() -> NO-OP (prevents accidental extra ticks)
 */
public final class LamportClock {
    private final String id;
    private final AtomicLong time = new AtomicLong(0);

    public LamportClock(String id) {
        this.id = id;
    }

    /** Current value without increment. */
    public long now() {
        return time.get();
    }

    /** Outbound event: increments once and returns the value to send. */
    public long send() {
        return time.incrementAndGet();
    }

    /** Inbound event: merge with remote and advance by +1. */
    public long receive(long remote) {
        if (remote < 0) remote = 0;
        while (true) {
            long cur = time.get();
            long next = Math.max(cur, remote) + 1;
            if (time.compareAndSet(cur, next)) return next;
        }
    }

    /** For legacy call sites: treat as NO-OP to avoid double-ticking. */
    public long tickProcess() {
        return now();
    }

    /** Legacy shim for previous API. */
    public long tickSend() {
        return send();
    }

    /** Legacy shim for previous API. */
    public void onReceive(long remote) {
        receive(remote);
    }

    /** Legacy name for read-only peek. */
    public long peek() {
        return now();
    }

    public String id() {
        return id;
    }
}
