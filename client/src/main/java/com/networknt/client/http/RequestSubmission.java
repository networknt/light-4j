package com.networknt.client.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Atomic admission/completion state; cancellation also reaches a late-created exchange. */
public final class RequestSubmission {
    private enum Phase { PENDING, STARTED, CANCELLED_BEFORE_SEND, CANCELLED_AFTER_SEND, FINISHED }
    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.PENDING);
    private final CompletableFuture<Throwable> cancellation = new CompletableFuture<>();

    boolean start() { return phase.compareAndSet(Phase.PENDING, Phase.STARTED); }
    public void finish() {
        phase.updateAndGet(previous -> previous == Phase.PENDING || previous == Phase.STARTED ? Phase.FINISHED : previous);
    }
    public boolean isCancelledBeforeSend() { return phase.get() == Phase.CANCELLED_BEFORE_SEND; }
    void onCancellation(Consumer<Throwable> listener) { cancellation.thenAccept(listener); }
    /** @return true only when cancellation wins against a started, unfinished submission. */
    public boolean cancel() { return cancel(new CancellationException("Request cancelled")); }
    /** @return true only when cancellation wins after native submission was admitted. */
    public boolean cancel(Throwable error) {
        for (;;) {
            Phase previous = phase.get();
            if (previous != Phase.PENDING && previous != Phase.STARTED) return false;
            Phase cancelled = previous == Phase.PENDING ? Phase.CANCELLED_BEFORE_SEND : Phase.CANCELLED_AFTER_SEND;
            if (phase.compareAndSet(previous, cancelled)) {
                cancellation.complete(error);
                return previous == Phase.STARTED;
            }
        }
    }
}
