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
        HttpServerExchange exchange = new HttpServerExchange(null);
        MiddlewareHandler middleware = new MiddlewareHandler() {
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(HttpServerExchange ex) { }
        };
        middleware.addHandlerMDCContext(exchange, "first", "one");
        var first = exchange.getAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT);
        assertThrows(UnsupportedOperationException.class, () -> first.put("bad", "mutation"));
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var updates = java.util.stream.IntStream.range(0, 20).mapToObj(i -> workers.submit(() ->
                    middleware.addHandlerMDCContext(exchange, "field" + i, "value" + i))).toList();
            for (var update : updates) update.get();
        }
        assertEquals(java.util.Map.of("first", "one"), first);
        assertEquals(21, exchange.getAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT).size());
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
            HttpServerExchange exchange = new HttpServerExchange(null);
            org.slf4j.MDC.put("worker", "prior");
            Error error = new OutOfMemoryError("injected sentinel, no memory pressure");
            Runnable task = HandlerContinuation.task(exchange,
                    HandlerContinuation.capture(exchange, ex -> { throw error; }));
            assertSame(error, assertThrows(OutOfMemoryError.class, task::run));
            assertEquals("prior", org.slf4j.MDC.get("worker"));
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
                HandlerContinuation.execute(exchange, (ex, task) -> { throw failure; }, ignored -> {})));
        assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(exchange)));
    }
}
