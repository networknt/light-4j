package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HandlerContinuationTest {
    @Test
    void activePolicyTracksIdentityAndExchangeAndRestoresAfterNestedFailure() throws Exception {
        HttpServerExchange first = new HttpServerExchange(null);
        HttpServerExchange second = new HttpServerExchange(null);
        var policy = HandlerContinuation.capture(first);
        var otherPolicy = HandlerContinuation.capture(first);
        assertFalse(HandlerContinuation.isActive(first, policy));
        policy.apply(ex -> {
            assertTrue(HandlerContinuation.isActive(first, policy));
            assertFalse(HandlerContinuation.isActive(first, otherPolicy));
            assertFalse(HandlerContinuation.isActive(second, policy));
            assertThrows(IOException.class, () -> policy.apply(nested -> {
                assertTrue(HandlerContinuation.isActive(second, policy));
                assertFalse(HandlerContinuation.isActive(first, policy));
                throw new IOException("nested failure");
            }).handleRequest(second));
            assertTrue(HandlerContinuation.isActive(first, policy));
        }).handleRequest(first);
        assertFalse(HandlerContinuation.isActive(first, policy));
        assertFalse(HandlerContinuation.isActive(second, policy));
    }

    @Test
    void mdcPublicationRetainsImmutableSnapshotsAndConcurrentUpdates() throws Exception {
        HttpServerExchange exchange = org.mockito.Mockito.spy(new HttpServerExchange(null));
        MiddlewareHandler middleware = new MiddlewareHandler() {
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(HttpServerExchange ex) { /* Publication-only fixture. */ }
        };
        middleware.addHandlerMDCContext(exchange, "first", "one");
        var holder = assertInstanceOf(RequestMdcContext.class,
                exchange.getAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT));
        var first = holder.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> first.put("bad", "mutation"));
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var updates = java.util.stream.IntStream.range(0, 20).mapToObj(i -> workers.submit(() ->
                    middleware.addHandlerMDCContext(exchange, "field" + i, "value" + i))).toList();
            for (var update : updates) update.get();
        }
        assertEquals(java.util.Map.of("first", "one"), first);
        assertEquals(21, holder.size());
        assertSame(holder, exchange.getAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT));
        org.mockito.Mockito.verify(exchange, org.mockito.Mockito.times(1)).putAttachment(
                org.mockito.Mockito.eq(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT), org.mockito.Mockito.any());
    }

    @Test
    void readersIterateStableSnapshotsWhileWritersPublishToInitializedHolder() throws Exception {
        HttpServerExchange exchange = org.mockito.Mockito.spy(new HttpServerExchange(null));
        RequestMdcContext holder = RequestMdcContext.initialize(exchange);
        holder.putFields(java.util.Map.of("epoch", "0", "mirror", "0"));
        var retained = holder.snapshot();
        var barrier = new java.util.concurrent.CyclicBarrier(4);
        var writersDone = new java.util.concurrent.CountDownLatch(2);
        var reads = new java.util.concurrent.atomic.AtomicInteger();
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
            for (int writer = 0; writer < 2; writer++) {
                int id = writer;
                tasks.add(workers.submit(() -> {
                    try {
                        barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                        for (int i = 1; i <= 2000; i++) {
                            String epoch = id + ":" + i;
                            holder.putFields(java.util.Map.of("epoch", epoch, "mirror", epoch));
                        }
                    } finally { writersDone.countDown(); }
                    return null;
                }));
            }
            for (int reader = 0; reader < 2; reader++) tasks.add(workers.submit(() -> {
                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                do {
                    var snapshot = holder.snapshot();
                    var copy = new java.util.HashMap<String, String>();
                    snapshot.forEach(copy::put);
                    assertEquals(copy.get("epoch"), copy.get("mirror"), "Never iterate a torn generation");
                    assertThrows(UnsupportedOperationException.class, () -> snapshot.put("bad", "mutation"));
                    reads.incrementAndGet();
                } while (writersDone.getCount() != 0);
                return null;
            }));
            for (var task : tasks) task.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertTrue(reads.get() >= 2);
        assertEquals(java.util.Map.of("epoch", "0", "mirror", "0"), retained,
                "A mutable HashMap implementation must fail this retained-generation assertion");
        assertSame(holder, exchange.getAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT));
        org.mockito.Mockito.verify(exchange, org.mockito.Mockito.times(1)).putAttachment(
                org.mockito.Mockito.eq(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT), org.mockito.Mockito.any());
    }

    @Test
    void failedFatalStatusWriteCannotReplaceOriginalError() throws Exception {
        Error fatal = new StackOverflowError("original");
        RuntimeException writeFailure = new IllegalStateException("status write failed");
        IOException closeFailure = new IOException("reset failed too");
        var connection = org.mockito.Mockito.mock(io.undertow.server.ServerConnection.class);
        org.mockito.Mockito.doThrow(closeFailure).when(connection).close();
        HttpServerExchange exchange = org.mockito.Mockito.spy(new HttpServerExchange(connection));
        org.mockito.Mockito.doThrow(writeFailure).when(exchange).endExchange();
        Error observed = assertThrows(StackOverflowError.class, () ->
                HandlerContinuation.task(exchange, ex -> { throw fatal; }).run());
        assertSame(fatal, observed);
        assertEquals(500, exchange.getStatusCode());
        assertArrayEquals(new Throwable[]{writeFailure, closeFailure}, fatal.getSuppressed());
        org.mockito.Mockito.verify(connection).close();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void resumeScopesCanonicalRequestMdcAndRestoresWorker(boolean present) throws Exception {
        var original = org.slf4j.MDC.getCopyOfContextMap();
        try {
            HttpServerExchange exchange = new HttpServerExchange(null);
            if (present) exchange.putAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT,
                    java.util.Map.of("request", "canonical"));
            org.slf4j.MDC.put("request", "stale-at-capture");
            HttpHandler resumed = HandlerContinuation.capture(exchange, ex -> {
                assertEquals(present ? "canonical" : null, org.slf4j.MDC.get("request"));
                assertNull(org.slf4j.MDC.get("worker"), "Do not borrow the executing thread's MDC");
                HandlerContinuation.capture(ex, nested -> {
                    assertEquals(present ? "canonical" : null, org.slf4j.MDC.get("request"));
                }).handleRequest(ex);
                throw new IOException("failure");
            });
            org.slf4j.MDC.clear();
            org.slf4j.MDC.put("worker", "unrelated");
            assertThrows(IOException.class, () -> resumed.handleRequest(exchange));
            assertEquals(java.util.Map.of("worker", "unrelated"), org.slf4j.MDC.getCopyOfContextMap());
        } finally {
            if (original == null) org.slf4j.MDC.clear();
            else org.slf4j.MDC.setContextMap(original);
        }
    }

    @Test
    void runnableAdapterPropagatesFatalErrorsAndRestoresMdc() {
        var original = org.slf4j.MDC.getCopyOfContextMap();
        try {
            HttpServerExchange exchange = org.mockito.Mockito.spy(new HttpServerExchange(null));
            org.mockito.Mockito.doReturn(exchange).when(exchange).endExchange();
            org.slf4j.MDC.put("worker", "prior");
            Error error = new OutOfMemoryError("injected sentinel, no memory pressure");
            Runnable task = HandlerContinuation.task(exchange,
                    HandlerContinuation.capture(exchange, ex -> { throw error; }));
            assertSame(error, assertThrows(OutOfMemoryError.class, task::run));
            assertEquals("prior", org.slf4j.MDC.get("worker"));
            org.mockito.Mockito.verify(exchange).endExchange();
            assertEquals(500, exchange.getStatusCode());
        } finally {
            if (original == null) org.slf4j.MDC.clear();
            else org.slf4j.MDC.setContextMap(original);
        }
    }

    @Test
    void nestedCaptureRestoresEnclosingBoundaryAndRetainsSelectedInstances() throws Exception {
        HttpServerExchange exchange = new HttpServerExchange(null);
        List<String> seen = new ArrayList<>();
        HandlerContinuation.Boundary outer = (ex, task) -> {
            seen.add("outer");
            task.handleRequest(ex);
        };
        HandlerContinuation.Boundary inner = (ex, task) -> {
            seen.add("inner");
            task.handleRequest(ex);
        };
        AtomicReference<HttpHandler> nested = new AtomicReference<>();
        AtomicReference<HttpHandler> enclosing = new AtomicReference<>();
        HandlerContinuation.execute(exchange, outer, ex -> {
            HandlerContinuation.execute(ex, inner, innerExchange ->
                    nested.set(HandlerContinuation.capture(innerExchange, ignored -> seen.add("nested"))));
            enclosing.set(HandlerContinuation.capture(ex, ignored -> seen.add("enclosing")));
        });
        seen.clear();
        nested.get().handleRequest(exchange);
        enclosing.get().handleRequest(exchange);
        assertEquals(List.of("inner", "nested", "outer", "enclosing"), seen);
        seen.clear();
        HandlerContinuation.capture(exchange, ignored -> seen.add("unwrapped")).handleRequest(exchange);
        assertEquals(List.of("unwrapped"), seen, "Neither frame may leave a boundary behind");
    }

    @Test
    void absentBoundaryStaysAbsentAndFailuresRestoreScope() throws Exception {
        HttpServerExchange exchange = new HttpServerExchange(null);
        IOException failure = new IOException("failure");
        HttpHandler unwrapped = HandlerContinuation.capture(exchange, ignored -> { throw failure; });
        HandlerContinuation.Boundary selected = (ex, task) -> {
            try { task.handleRequest(ex); }
            catch (IOException e) { fail("A captured absent boundary must hide the active boundary", e); }
        };
        HandlerContinuation.execute(exchange, selected, ex ->
                assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(ex))));
        assertSame(failure, assertThrows(IOException.class, () ->
                HandlerContinuation.execute(exchange, (ex, task) -> { throw failure; }, ignored -> { /* The boundary throws before this continuation runs. */ })));
        assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(exchange)));
    }
}
