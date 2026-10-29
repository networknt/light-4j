package com.networknt.client.http;

import java.util.concurrent.atomic.AtomicReference;

/** Admission/cancellation state only: no locks, callbacks, or native operations. */
public final class RequestSubmission {
    private enum Phase { PENDING, STARTED, CANCELLED, FINISHED }
    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.PENDING);
    boolean start() { return phase.compareAndSet(Phase.PENDING, Phase.STARTED); }
    public void finish() { phase.set(Phase.FINISHED); }
    /** @return true only when cancellation wins against a started, unfinished submission. */
    public boolean cancel() {
        for (;;) {
            Phase previous = phase.get();
            if (previous == Phase.CANCELLED || previous == Phase.FINISHED) return false;
            if (phase.compareAndSet(previous, Phase.CANCELLED)) return previous == Phase.STARTED;
        }
    }
}
