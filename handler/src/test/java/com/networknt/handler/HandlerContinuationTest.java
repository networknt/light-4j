package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HandlerWrapper;
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
    void offFrameCaptureRetainsEntryPolicyAndHolderOnAnotherThread() throws Exception {
        var exchange = spy(new HttpServerExchange(null));
        var selected = new AtomicInteger();
        var saved = new AtomicReference<HandlerContinuation.Scope>();
        HandlerContinuation.execute(exchange, next -> ex -> {
            try { next.handleRequest(ex); }
            catch (IOException _) { selected.incrementAndGet(); }
        }, ex -> {
            publicationMiddleware().addHandlerMDCContext(ex, "request", "entry");
            saved.set(HandlerContinuation.capture(ex));
        });
        var holder = RequestMdcContext.initialize(exchange);
        clearInvocations(exchange);
        try (var worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                assertSame(holder, RequestMdcContext.initialize(exchange));
                HttpHandler failing = ex -> {
                    assertEquals("entry", MDC.get("request"));
                    throw new IOException("off-frame failure");
                };
                assertThrows(IOException.class, () -> HandlerContinuation.capture(exchange, failing).handleRequest(exchange),
                        "An exchange alone cannot infer lexical policy outside its frame");
                saved.get().wrap(failing).handleRequest(exchange);
                return null;
            }).get(5, TimeUnit.SECONDS);
        }
        assertEquals(1, selected.get());
        verify(exchange, never()).getAttachment(any());
        verify(exchange, never()).putAttachment(any(), any());
    }

    @Test
    void deferredBoundaryRetainsItsContinuationAndRestoresWorkerMdc() throws Exception {
        var exchange = new HttpServerExchange(null);
        var pending = new AtomicReference<HttpHandler>();
        var saved = new AtomicReference<HandlerContinuation.Scope>();
        HandlerContinuation.execute(exchange, next -> ex -> pending.set(next), ex -> {
            publicationMiddleware().addHandlerMDCContext(ex, "request", "deferred");
            saved.set(HandlerContinuation.capture(ex));
        });
        pending.getAndSet(null).handleRequest(exchange);
        var calls = new AtomicInteger();
        saved.get().task(ex -> {
            assertEquals("deferred", MDC.get("request"));
            calls.incrementAndGet();
        }).run();
        assertEquals(0, calls.get());
        try (var worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                MDC.put("worker", "prior");
                try {
                    pending.getAndSet(null).handleRequest(exchange);
                    assertEquals("prior", MDC.get("worker"));
                } finally { MDC.clear(); }
                return null;
            }).get(5, TimeUnit.SECONDS);
        }
        assertEquals(1, calls.get());
    }

    @Test
    void inheritedExchangeOwnsItsMdcAndUsesTheSelectedPolicy() throws Exception {
        var parent = new HttpServerExchange(null);
        var child = new HttpServerExchange(null);
        var policy = new AtomicReference<HandlerContinuation.Scope>();
        var seen = new ArrayList<HttpServerExchange>();
        HandlerContinuation.execute(parent, next -> ex -> { seen.add(ex); next.handleRequest(ex); }, ex -> {
            publicationMiddleware().addHandlerMDCContext(ex, "request", "parent");
            policy.set(HandlerContinuation.capture(ex));
        });
        seen.clear();
        policy.get().fork(child).wrap(ex -> {
            assertSame(child, ex);
            assertEquals("parent", MDC.get("request"));
            publicationMiddleware().addHandlerMDCContext(ex, "request", "child");
        }).handleRequest(child);
        assertEquals(List.of(child), seen);
        assertEquals("parent", RequestMdcContext.initialize(parent).snapshot().get("request"));
        assertEquals("child", RequestMdcContext.initialize(child).snapshot().get("request"));
    }

    @Test
    void activePolicyTracksIdentityAndExchangeAndRestoresAfterNestedFailure() throws Exception {
        HttpServerExchange first = new HttpServerExchange(null);
        HttpServerExchange second = new HttpServerExchange(null);
        var policy = HandlerContinuation.capture(first);
        var otherPolicy = HandlerContinuation.capture(first);
        assertFalse(policy.isActive(first));
        policy.wrap(ex -> {
            assertTrue(policy.isActive(first));
            assertFalse(otherPolicy.isActive(first));
            assertFalse(policy.isActive(second));
            var child = policy.fork(second);
            assertThrows(IOException.class, () -> child.wrap(nested -> {
                assertTrue(child.isActive(second));
                assertFalse(policy.isActive(first));
                throw new IOException("nested failure");
            }).handleRequest(second));
            assertTrue(policy.isActive(first));
        }).handleRequest(first);
        assertFalse(policy.isActive(first));
        assertFalse(policy.isActive(second));
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
        HandlerContinuation.execute(exchange, next -> ex -> next.handleRequest(ex), ex -> {
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

    @Test
    void earlierNarrowPolicyCannotShadowLaterPolicyOrOffFrameCapture() throws Exception {
        var exchange = new HttpServerExchange(null);
        var narrow = new AtomicReference<HandlerContinuation.Scope>();
        var later = new AtomicReference<HandlerContinuation.Scope>();
        var seen = new ArrayList<String>();
        HandlerWrapper first = next -> ex -> {
            try { next.handleRequest(ex); }
            catch (IOException _) { seen.add("narrow"); }
        };
        HandlerWrapper second = next -> ex -> {
            try { next.handleRequest(ex); }
            catch (IOException _) { seen.add("later"); }
        };
        HandlerContinuation.execute(exchange, first, ex -> narrow.set(HandlerContinuation.capture(ex)));
        var outside = HandlerContinuation.capture(exchange);
        HandlerContinuation.execute(exchange, second, ex -> later.set(HandlerContinuation.capture(ex)));
        HttpHandler fail = ex -> { throw new IOException("selected failure"); };
        assertThrows(IOException.class, () -> outside.wrap(fail).handleRequest(exchange));
        narrow.get().wrap(fail).handleRequest(exchange);
        later.get().wrap(fail).handleRequest(exchange);
        assertEquals(List.of("narrow", "later"), seen);
    }

    @Test
    void dispatchPreservesForeignArrivalBeforeOwnerArrival() throws Exception {
        var exchange = new HttpServerExchange(null);
        var scheduled = new LinkedBlockingQueue<Runnable>();
        Thread owner = Thread.currentThread();
        var dispatcher = HandlerContinuation.capture(exchange).dispatcher(scheduled::add,
                () -> Thread.currentThread() == owner, () -> fail("Unexpected dispatch rejection"));
        var seen = new ArrayList<String>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> dispatcher.execute(ex -> seen.add("foreign"))).get(5, TimeUnit.SECONDS);
        }
        dispatcher.execute(ex -> seen.add("owner"));
        assertTrue(seen.isEmpty(), "Owner arrival must not overtake an already queued foreign arrival");
        scheduled.take().run();
        assertEquals(List.of("foreign", "owner"), seen);
        assertTrue(scheduled.isEmpty());
    }

    @Test
    void rejectedDispatchUsesSelectedPolicyAndRetiresEachResource() throws Exception {
        var exchange = new HttpServerExchange(null);
        var scope = new AtomicReference<HandlerContinuation.Scope>();
        var failures = new AtomicInteger();
        var cleanup = new AtomicInteger();
        var abort = new AtomicInteger();
        HandlerContinuation.execute(exchange, next -> ex -> {
            try { next.handleRequest(ex); }
            catch (RejectedExecutionException _) { failures.incrementAndGet(); }
        }, ex -> scope.set(HandlerContinuation.capture(ex)));
        var dispatcher = scope.get().dispatcher(task -> {
            throw new RejectedExecutionException("executor stopped");
        }, () -> false, abort::incrementAndGet);
        dispatcher.execute(ex -> fail("Rejected callback must not run"), cleanup::incrementAndGet);
        dispatcher.execute(ex -> fail("Stopped dispatcher must not run"), cleanup::incrementAndGet);
        assertEquals(1, failures.get());
        assertEquals(1, abort.get());
        assertEquals(2, cleanup.get());
    }

    @Test
    void acceptedWorkRunsOnceAndItsFailureUsesSelectedPolicy() throws Exception {
        var exchange = new HttpServerExchange(null);
        var scope = new AtomicReference<HandlerContinuation.Scope>();
        var failure = new IOException("accepted failure");
        var observed = new AtomicReference<Exception>();
        var calls = new AtomicInteger();
        HandlerContinuation.execute(exchange, next -> ex -> {
            try { next.handleRequest(ex); }
            catch (IOException caught) { observed.set(caught); }
        }, ex -> scope.set(HandlerContinuation.capture(ex)));
        scope.get().resume(ex -> { calls.incrementAndGet(); throw failure; });
        assertSame(failure, observed.get());
        assertEquals(1, calls.get());
    }

    @Test
    void captureDoesNotApplyWrapperUntilExecution() throws Exception {
        var exchange = new HttpServerExchange(null);
        var applications = new AtomicInteger();
        var captured = new AtomicReference<HttpHandler>();
        HandlerWrapper boundary = next -> {
            applications.incrementAndGet();
            return next;
        };
        HandlerContinuation.execute(exchange, boundary, ex -> {
            captured.set(HandlerContinuation.capture(ex, resumed -> assertSame(exchange, resumed)));
            assertEquals(1, applications.get());
        });
        captured.get().handleRequest(exchange);
        assertEquals(2, applications.get());
    }

    @Test
    void completionRemovesIndexEntryWhileRetainedScopeKeepsItsMdc() throws Exception {
        var completed = new CountDownLatch(1);
        var captured = new AtomicReference<HandlerContinuation.Scope>();
        var request = new AtomicReference<HttpServerExchange>();
        var holder = new AtomicReference<RequestMdcContext>();
        var server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(exchange -> {
            publicationMiddleware().addHandlerMDCContext(exchange, "request", "completed");
            request.set(exchange);
            holder.set(RequestMdcContext.initialize(exchange));
            captured.set(HandlerContinuation.capture(exchange));
            assertNotNull(RequestState.find(exchange));
            exchange.addExchangeCompleteListener((ex, next) -> {
                next.proceed();
                completed.countDown();
            });
            exchange.getResponseSender().send("done");
        }).build();
        try (var client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/lifetime")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals("done", response.body());
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertNull(RequestState.find(request.get()));
            assertSame(holder.get(), RequestMdcContext.initialize(request.get()));
            captured.get().wrap(ex -> assertEquals("completed", MDC.get("request"))).handleRequest(request.get());
            assertNull(RequestState.find(request.get()), "Late MDC access must not reinsert a completed request");
        } finally { server.stop(); }
    }

    @Test
    void fatalDispatchRetiresQueuedAndFutureCallbacksWithoutMaskingError() {
        var exchange = spy(new HttpServerExchange(null));
        doReturn(exchange).when(exchange).endExchange();
        var retired = new AtomicInteger();
        var aborted = new AtomicInteger();
        var fatal = new StackOverflowError("injected callback sentinel");
        var dispatcher = HandlerContinuation.capture(exchange).dispatcher(Runnable::run, () -> true,
                aborted::incrementAndGet);
        Error observed = assertThrows(StackOverflowError.class, () -> dispatcher.execute(ex -> {
            dispatcher.execute(queued -> fail("Queued callback survived fatal failure"), () -> {
                retired.incrementAndGet();
                throw new IllegalStateException("cleanup failed too");
            });
            throw fatal;
        }));
        assertSame(fatal, observed);
        dispatcher.execute(ex -> fail("Future callback survived fatal failure"), retired::incrementAndGet);
        assertEquals(2, retired.get());
        assertEquals(1, aborted.get());
        verify(exchange).endExchange();
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
        verify(exchange).setStatusCode(500);
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
        HandlerContinuation.execute(exchange, next -> ex -> { callsA.incrementAndGet(); next.handleRequest(ex); }, ex ->
                a.set(HandlerContinuation.capture(ex, resumed -> {
                    aEntered.countDown();
                    assertTrue(bEntered.await(5, TimeUnit.SECONDS));
                    HandlerContinuation.capture(resumed, child -> { /* Exercise the captured policy. */ }).handleRequest(resumed);
                    aCaptured.countDown();
                })));
        HandlerContinuation.execute(exchange, next -> ex -> { callsB.incrementAndGet(); next.handleRequest(ex); }, ex ->
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
        verify(exchange).endExchange();
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
        HandlerWrapper outer = task -> ex -> {
            seen.add("outer");
            task.handleRequest(ex);
        };
        HandlerWrapper inner = task -> ex -> {
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
        assertEquals(List.of("unwrapped"), seen, "Lexical policy ends with its frame; retain a Scope across hand-off");
    }

    @Test
    void absentBoundaryStaysAbsentAndFailuresRestoreScope() throws Exception {
        HttpServerExchange exchange = new HttpServerExchange(null);
        IOException failure = new IOException("failure");
        HttpHandler unwrapped = HandlerContinuation.capture(exchange, ignored -> { throw failure; });
        HandlerWrapper selected = task -> ex -> {
            assertDoesNotThrow(() -> task.handleRequest(ex),
                    "A captured absent boundary must hide the active boundary");
        };
        HandlerContinuation.execute(exchange, selected, ex ->
                assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(ex))));
        assertSame(failure, assertThrows(IOException.class, () ->
                HandlerContinuation.execute(exchange, task -> ex -> { throw failure; }, ignored -> { /* The boundary throws before this continuation runs. */ })));
        assertSame(failure, assertThrows(IOException.class, () -> unwrapped.handleRequest(exchange)));
    }
}
