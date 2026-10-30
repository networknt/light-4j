package com.networknt.handler;

import io.undertow.server.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Proxy-local FIFO. Transport callbacks and ownerFailure run only on the owning I/O thread. */
final class ProxyCallbackDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyCallbackDispatcher.class);
    private static final Runnable NO_RESOURCE = () -> { };
    private record Event(HttpHandler callback, Runnable discardedResource, boolean accepted) { }

    private final HandlerContinuation.Scope scope;
    private final Executor executor;
    private final BooleanSupplier inOwner;
    private final Runnable ownerFailure;
    private final Consumer<RejectedExecutionException> rejectedExecutor;
    private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger work = new AtomicInteger();
    private final AtomicReference<Throwable> terminal = new AtomicReference<>();
    private final Runnable drainTask = this::drain;

    ProxyCallbackDispatcher(HandlerContinuation.Scope scope, Executor executor, BooleanSupplier inOwner,
                            Runnable ownerFailure, Consumer<RejectedExecutionException> rejectedExecutor) {
        this.scope = scope;
        this.executor = executor;
        this.inOwner = inOwner;
        this.ownerFailure = ownerFailure;
        this.rejectedExecutor = rejectedExecutor;
    }

    void execute(HttpHandler callback) { execute(callback, NO_RESOURCE); }

    /** discardedResource may run on any submitting thread: close only this incoming event's resource. */
    void execute(HttpHandler callback, Runnable discardedResource) { submit(callback, discardedResource, false); }

    void resume(HttpHandler callback, Runnable discardedResource) { submit(callback, discardedResource, true); }

    private void submit(HttpHandler callback, Runnable discardedResource, boolean accepted) {
        queue.add(new Event(callback, discardedResource, accepted));
        Throwable failure = terminal.get();
        if (failure != null) { discardQueued(failure); return; }
        if (work.getAndIncrement() != 0) return;
        if (inOwner.getAsBoolean()) drain();
        else {
            try { executor.execute(drainTask); }
            catch (RejectedExecutionException rejected) {
                if (terminal.compareAndSet(null, rejected)) {
                    discardQueued(rejected);
                    // Exactly the rejecting submission owns this terminal notification.
                    // It must use a published resource snapshot, never mutable proxy fields.
                    rejectedExecutor.accept(rejected);
                }
            }
        }
    }

    private void drain() {
        int missed = 1;
        try {
            do {
                Event event;
                while ((event = queue.poll()) != null) {
                    if (event.accepted()) scope.resume(event.callback());
                    else scope.execute(event.callback());
                }
                missed = work.addAndGet(-missed);
            } while (missed != 0);
        } catch (Error failure) {
            // This path is still on the owner. Stop before retiring queued resources.
            cleanup(ownerFailure, failure);
            terminal.compareAndSet(null, failure);
            discardQueued(failure);
            throw failure;
        }
    }

    private void discardQueued(Throwable failure) {
        Event event;
        while ((event = queue.poll()) != null) cleanup(event.discardedResource(), failure);
    }

    static void cleanup(Runnable cleanup, Throwable primary) {
        try { cleanup.run(); }
        catch (Throwable failure) {
            if (primary == failure) return;
            try {
                primary.addSuppressed(failure);
                LOG.error("Proxy callback resource cleanup failed", failure);
            } catch (Throwable _) {
                // Best-effort diagnostics must not mask the original terminal failure.
            }
        }
    }
}
