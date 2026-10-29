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
    void resumedFrameCarriesRequestMdcAndRestoresCallingThreadEvenOnFailure() throws Exception {
        var original = org.slf4j.MDC.getCopyOfContextMap();
        try {
            HttpServerExchange exchange = new HttpServerExchange(null);
            org.slf4j.MDC.put("request", "captured");
            HttpHandler resumed = HandlerContinuation.capture(exchange, ex -> {
                assertEquals("captured", org.slf4j.MDC.get("request"));
                org.slf4j.MDC.clear();
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
