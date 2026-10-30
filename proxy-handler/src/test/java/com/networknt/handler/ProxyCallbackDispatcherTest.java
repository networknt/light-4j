package com.networknt.handler;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ProxyCallbackDispatcherTest {
    @Test
    void foreignArrivalCannotBeOvertakenByOwnerArrival() throws Exception {
        var exchange = new HttpServerExchange(null);
        var scheduled = new LinkedBlockingQueue<Runnable>();
        Thread owner = Thread.currentThread();
        var queue = new ProxyCallbackDispatcher(HandlerContinuation.capture(exchange), scheduled::add,
                () -> Thread.currentThread() == owner, () -> fail("Unexpected owner failure"),
                failure -> fail("Unexpected rejection", failure));
        var seen = new ArrayList<String>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> queue.execute(ex -> seen.add("foreign"))).get(5, TimeUnit.SECONDS);
        }
        queue.execute(ex -> seen.add("owner"));
        assertTrue(seen.isEmpty());
        scheduled.take().run();
        assertEquals(List.of("foreign", "owner"), seen);
        assertTrue(scheduled.isEmpty());
    }

    @Test
    void onlyRejectingSubmissionNotifiesPolicyAndNeverCallsOwnerCleanup() throws Exception {
        var exchange = new HttpServerExchange(null);
        var selected = new AtomicReference<HandlerContinuation.Scope>();
        var policyCalls = new AtomicInteger();
        HandlerContinuation.execute(exchange, next -> ex -> {
            try { next.handleRequest(ex); }
            catch (RejectedExecutionException _) { policyCalls.incrementAndGet(); }
        }, ex -> selected.set(HandlerContinuation.capture(ex)));
        var rejectThread = new AtomicReference<Thread>();
        var retired = new AtomicInteger();
        var cleanupFailure = new IllegalStateException("isolated resource cleanup");
        var rejection = new RejectedExecutionException("owner executor stopped");
        var queue = new ProxyCallbackDispatcher(selected.get(), task -> { throw rejection; },
                () -> false, () -> fail("Mutable owner cleanup ran on a rejecting submitter"), failure -> {
                    assertTrue(rejectThread.compareAndSet(null, Thread.currentThread()));
                    selected.get().execute(ex -> { throw failure; });
                });
        try (var workers = Executors.newFixedThreadPool(4)) {
            Thread first = workers.submit(() -> {
                queue.execute(ex -> fail("Rejected work ran"), () -> {
                    retired.incrementAndGet();
                    throw cleanupFailure;
                });
                return Thread.currentThread();
            }).get(5, TimeUnit.SECONDS);
            var futureSubmissions = new ArrayList<java.util.concurrent.Future<?>>();
            for (int n = 0; n < 100; n++)
                futureSubmissions.add(workers.submit(() -> queue.execute(ex -> fail("Stopped work ran"),
                        retired::incrementAndGet)));
            for (var future : futureSubmissions) future.get(5, TimeUnit.SECONDS);
            assertSame(first, rejectThread.get());
        }
        assertEquals(1, policyCalls.get());
        assertEquals(101, retired.get());
        assertArrayEquals(new Throwable[]{cleanupFailure}, rejection.getSuppressed());
    }

    @Test
    void concurrentProducersPreserveTheirOrderAndDoNotLoseIdleWakeups() throws Exception {
        var exchange = new HttpServerExchange(null);
        var owner = new AtomicReference<Thread>();
        var problem = new AtomicReference<String>();
        try (var executor = Executors.newSingleThreadExecutor(task -> new Thread(() -> {
            owner.set(Thread.currentThread());
            task.run();
        })); var producers = Executors.newFixedThreadPool(4)) {
            var queue = new ProxyCallbackDispatcher(HandlerContinuation.capture(exchange), executor,
                    () -> Thread.currentThread() == owner.get(),
                    () -> problem.compareAndSet(null, "owner failure"),
                    failure -> problem.compareAndSet(null, "unexpected rejection"));
            int[] expected = new int[4];
            for (int round = 0; round < 40; round++) {
                var submitted = new ArrayList<java.util.concurrent.Future<?>>();
                int base = round * 200;
                for (int producer = 0; producer < 4; producer++) {
                    int id = producer;
                    submitted.add(producers.submit(() -> {
                        for (int n = base; n < base + 200; n++) {
                            int sequence = n;
                            queue.execute(ex -> {
                                if (Thread.currentThread() != owner.get())
                                    problem.compareAndSet(null, "callback escaped owner");
                                if (expected[id]++ != sequence)
                                    problem.compareAndSet(null, "producer order changed");
                            });
                            if ((n & 7) == 0) Thread.yield();
                        }
                    }));
                }
                for (var future : submitted) future.get(5, TimeUnit.SECONDS);
                var drained = new CompletableFuture<Void>();
                queue.execute(ex -> drained.complete(null));
                drained.get(5, TimeUnit.SECONDS);
            }
            assertArrayEquals(new int[]{8000, 8000, 8000, 8000}, expected);
            assertNull(problem.get(), problem.get());
        }
    }

    @Test
    void fatalOwnerFailureRetiresQueuedAndForeignEventsAndRetainsDiagnostics() throws Exception {
        var fatal = new StackOverflowError("injected fatal callback");
        var cleanupFailure = new IllegalStateException("queued resource cleanup");
        var observed = new CompletableFuture<Error>();
        var dispatcher = new CompletableFuture<ProxyCallbackDispatcher>();
        var ownerThread = new AtomicReference<Thread>();
        var cleanupThread = new AtomicReference<Thread>();
        var ownerAborts = new AtomicInteger();
        var retired = new AtomicInteger();
        var server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1).setHandler(exchange -> {
            ownerThread.set(Thread.currentThread());
            var queue = new ProxyCallbackDispatcher(HandlerContinuation.capture(exchange), exchange.getIoThread(),
                    exchange::isInIoThread, () -> {
                        cleanupThread.set(Thread.currentThread());
                        ownerAborts.incrementAndGet();
                    }, failure -> observed.completeExceptionally(failure));
            dispatcher.complete(queue);
            try {
                queue.execute(ex -> {
                    queue.execute(queued -> fail("Queued transport work survived fatal error"), () -> {
                        retired.incrementAndGet();
                        throw cleanupFailure;
                    });
                    throw fatal;
                });
            } catch (Error failure) { observed.complete(failure); }
        }).build();
        try (var client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/fatal"))
                    .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
            assertSame(fatal, observed.get(5, TimeUnit.SECONDS));
            try (var producers = Executors.newFixedThreadPool(4)) {
                var futures = new ArrayList<java.util.concurrent.Future<?>>();
                for (int n = 0; n < 100; n++)
                    futures.add(producers.submit(() -> dispatcher.join().execute(ex -> fail("Stopped transport ran"),
                            retired::incrementAndGet)));
                for (var future : futures) future.get(5, TimeUnit.SECONDS);
            }
            assertSame(ownerThread.get(), cleanupThread.get());
            assertEquals(1, ownerAborts.get());
            assertEquals(101, retired.get());
            assertArrayEquals(new Throwable[]{cleanupFailure}, fatal.getSuppressed());
        } finally { server.stop(); }
    }
}
