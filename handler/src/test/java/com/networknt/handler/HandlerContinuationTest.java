package com.networknt.handler;


import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        HttpServerExchange exchange = spy(new HttpServerExchange(null));
        MiddlewareHandler middleware = new MiddlewareHandler() {
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(HttpServerExchange ex) { /* Publication-only fixture. */ }
        };
        middleware.addHandlerMDCContext(exchange, "first", "one");
        var holder = assertInstanceOf(RequestMdcContext.class,
                exchange.getAttachment(AttachmentConstants.MDC_CONTEXT));
        var first = holder.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> first.put("bad", "mutation"));
        try (var workers = Executors.newFixedThreadPool(4)) {
            var updates = java.util.stream.IntStream.range(0, 20).mapToObj(i -> workers.submit(() ->
                    middleware.addHandlerMDCContext(exchange, "field" + i, "value" + i))).toList();
            for (var update : updates) update.get();
        }
        assertEquals(Map.of("first", "one"), first);
        assertEquals(21, holder.size());
        assertSame(holder, exchange.getAttachment(AttachmentConstants.MDC_CONTEXT));
        verify(exchange, times(1)).putAttachment(
                eq(AttachmentConstants.MDC_CONTEXT), any());
    }

    @Test
    void productionResumesReadStableSnapshotsWhileWritersPublish() throws Exception {
        HttpServerExchange exchange = spy(new HttpServerExchange(null));
        exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, Map.of("epoch", "0", "legacy", "retained"));
        MiddlewareHandler middleware = publicationMiddleware();
        AtomicReference<RequestMdcContext> context = new AtomicReference<>();
        AtomicReference<HttpHandler> writer = new AtomicReference<>();
        AtomicReference<HttpHandler> reader = new AtomicReference<>();
        CountDownLatch readerHoldingSnapshot = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        HandlerContinuation.execute(exchange, (ex, next) -> next.handleRequest(ex), ex -> {
            context.set(RequestMdcContext.initialize(ex));
            reader.set(HandlerContinuation.capture(ex, resumed -> {
                assertEquals("retained", MDC.get("legacy"));
                var generation = context.get().snapshot();
                var iterator = generation.entrySet().iterator();
                assertTrue(iterator.hasNext());
                var first = iterator.next();
                readerHoldingSnapshot.countDown();
                assertTrue(updated.await(5, TimeUnit.SECONDS));
                var copy = new HashMap<String, String>();
                copy.put(first.getKey(), first.getValue());
                iterator.forEachRemaining(entry -> copy.put(entry.getKey(), entry.getValue()));
                assertEquals("0", copy.get("epoch"), "A retained generation must not change during publication");
                assertThrows(UnsupportedOperationException.class, () -> generation.put("bad", "mutation"));
            }));
            writer.set(HandlerContinuation.capture(ex, resumed -> {
                assertTrue(readerHoldingSnapshot.await(5, TimeUnit.SECONDS));
                for (int i = 1; i <= 2000; i++) middleware.addHandlerMDCContext(resumed, "epoch", Integer.toString(i));
                updated.countDown();
            }));
        });
        clearInvocations(exchange);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var reading = workers.submit(() -> { reader.get().handleRequest(exchange); return null; });
            var writing = workers.submit(() -> { writer.get().handleRequest(exchange); return null; });
            reading.get(10, TimeUnit.SECONDS);
            writing.get(10, TimeUnit.SECONDS);
        }
        // Both production resumes and helper updates reuse the captured holder without attachment access.
        verify(exchange, never()).getAttachment(AttachmentConstants.MDC_CONTEXT);
        verify(exchange, never()).putAttachment(eq(AttachmentConstants.MDC_CONTEXT), any());
        reader.set(HandlerContinuation.capture(exchange, resumed -> assertEquals("2000", MDC.get("epoch"))));
        reader.get().handleRequest(exchange);
        assertEquals("retained", context.get().snapshot().get("legacy"));
    }

    private static MiddlewareHandler publicationMiddleware() {
        return new MiddlewareHandler() {
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(HttpServerExchange ex) { /* Publication-only fixture. */ }
        };
    }

    @Test
    void legacyContextInRawRunnableIsMigratedBeforeItRuns() throws Exception {
        HttpServerExchange exchange = spy(new HttpServerExchange(null));
        exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, Map.of("legacy", "value"));
        var callback = HandlerContinuation.task(exchange, ex -> assertEquals("value", MDC.get("legacy")));
        clearInvocations(exchange);
        try (var workers = Executors.newSingleThreadExecutor()) { workers.submit(callback).get(5, TimeUnit.SECONDS); }
        verify(exchange, never()).getAttachment(AttachmentConstants.MDC_CONTEXT);
        verify(exchange, never()).putAttachment(eq(AttachmentConstants.MDC_CONTEXT), any());
    }

    @Test
    void exceptionCleanupPreservesFailureWhenStatusWriteAndResetThrow() throws Exception {
        IOException original = new IOException("callback failed");
        RuntimeException writeFailure = new IllegalStateException("write failed");
        RuntimeException closeFailure = new IllegalStateException("close failed");
        var connection = mock(ServerConnection.class);
        doThrow(closeFailure).when(connection).close();
        var exchange = spy(new HttpServerExchange(connection));
        doThrow(writeFailure).when(exchange).setStatusCode(500);
        assertDoesNotThrow(() -> HandlerContinuation.task(exchange, ex -> { throw original; }).run());
        assertArrayEquals(new Throwable[]{writeFailure, closeFailure}, original.getSuppressed());
        verify(connection).close();
    }

    @Test
    void overlappingFramesKeepTheirOwnPolicyWithoutAttachmentAccess() throws Exception {
        var exchange = spy(new HttpServerExchange(null));
        var aEntered = new CountDownLatch(1);
        var bEntered = new CountDownLatch(1);
        var aCaptured = new CountDownLatch(1);
        var callsA = new AtomicInteger();
        var callsB = new AtomicInteger();
        var a = new AtomicReference<HttpHandler>();
        var b = new AtomicReference<HttpHandler>();
        HandlerContinuation.execute(exchange, (ex, next) -> { callsA.incrementAndGet(); next.handleRequest(ex); }, ex ->
                a.set(HandlerContinuation.capture(ex, resumed -> {
                    aEntered.countDown();
                    assertTrue(bEntered.await(5, TimeUnit.SECONDS));
                    HandlerContinuation.capture(resumed, child -> { /* Exercise the captured policy. */ }).handleRequest(resumed);
                    aCaptured.countDown();
                })));
        HandlerContinuation.execute(exchange, (ex, next) -> { callsB.incrementAndGet(); next.handleRequest(ex); }, ex ->
                b.set(HandlerContinuation.capture(ex, resumed -> {
                    assertTrue(aEntered.await(5, TimeUnit.SECONDS));
                    bEntered.countDown();
                    assertTrue(aCaptured.await(5, TimeUnit.SECONDS));
                })));
        callsA.set(0);
        callsB.set(0);
        clearInvocations(exchange);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { a.get().handleRequest(exchange); return null; });
            var second = workers.submit(() -> { b.get().handleRequest(exchange); return null; });
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(2, callsA.get());
        assertEquals(1, callsB.get());
        verify(exchange, never()).getAttachment(any());
        verify(exchange, never()).putAttachment(any(), any());
        verify(exchange, never()).removeAttachment(any());
    }

    @Test
    void failedFatalStatusWriteCannotReplaceOriginalError() throws Exception {
        Error fatal = new StackOverflowError("original");
        RuntimeException writeFailure = new IllegalStateException("status write failed");
        IOException closeFailure = new IOException("reset failed too");
        var connection = mock(ServerConnection.class);
        doThrow(closeFailure).when(connection).close();
        HttpServerExchange exchange = spy(new HttpServerExchange(connection));
        doThrow(writeFailure).when(exchange).endExchange();
        Runnable callback = HandlerContinuation.task(exchange, ex -> { throw fatal; });
        Error observed = assertThrows(StackOverflowError.class, callback::run);
        assertSame(fatal, observed);
        assertEquals(500, exchange.getStatusCode());
        assertArrayEquals(new Throwable[]{writeFailure, closeFailure}, fatal.getSuppressed());
        verify(connection).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resumeScopesCanonicalRequestMdcAndRestoresWorker(boolean present) throws Exception {
        var original = MDC.getCopyOfContextMap();
        try {
            HttpServerExchange exchange = new HttpServerExchange(null);
            if (present) exchange.putAttachment(AttachmentConstants.MDC_CONTEXT,
                    Map.of("request", "canonical"));
            MDC.put("request", "stale-at-capture");
            HttpHandler resumed = HandlerContinuation.capture(exchange, ex -> {
                assertEquals(present ? "canonical" : null, MDC.get("request"));
                assertNull(MDC.get("worker"), "Do not borrow the executing thread's MDC");
                HandlerContinuation.capture(ex, nested -> {
                    assertEquals(present ? "canonical" : null, MDC.get("request"));
                }).handleRequest(ex);
                throw new IOException("failure");
            });
            MDC.clear();
            MDC.put("worker", "unrelated");
            assertThrows(IOException.class, () -> resumed.handleRequest(exchange));
            assertEquals(Map.of("worker", "unrelated"), MDC.getCopyOfContextMap());
        } finally {
            if (original == null) MDC.clear();
            else MDC.setContextMap(original);
        }
    }

    @Test
    void runnableAdapterPropagatesFatalErrorsAndRestoresMdc() {
        var original = MDC.getCopyOfContextMap();
        try {
            HttpServerExchange exchange = spy(new HttpServerExchange(null));
            doReturn(exchange).when(exchange).endExchange();
            MDC.put("worker", "prior");
            Error error = new OutOfMemoryError("injected sentinel, no memory pressure");
            Runnable task = HandlerContinuation.task(exchange,
                    HandlerContinuation.capture(exchange, ex -> { throw error; }));
            assertSame(error, assertThrows(OutOfMemoryError.class, task::run));
            assertEquals("prior", MDC.get("worker"));
            verify(exchange).endExchange();
            assertEquals(500, exchange.getStatusCode());
        } finally {
            if (original == null) MDC.clear();
            else MDC.setContextMap(original);
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
            assertDoesNotThrow(() -> task.handleRequest(ex),
                    "A captured absent boundary must hide the active boundary");
        };
        HandlerContinuation.execute(exchange, selected, ex ->
                assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(ex))));
        assertSame(failure, assertThrows(IOException.class, () ->
                HandlerContinuation.execute(exchange, (ex, task) -> { throw failure; }, ignored -> { /* The boundary throws before this continuation runs. */ })));
        assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(exchange)));
    }
}
