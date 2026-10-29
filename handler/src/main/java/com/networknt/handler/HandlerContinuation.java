package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

/**
 * Carries the active exception boundary across an asynchronous hand-off. Boundary
 * state belongs to an exchange, never a thread or a shared middleware instance.
 * Capture before returning from the current chain frame; invoke the captured
 * handler through Undertow's dispatch or root-handler machinery.
 */
public final class HandlerContinuation {
    private static final AttachmentKey<Boundary> ACTIVE = AttachmentKey.create(Boundary.class);

    private HandlerContinuation() { }

    /** A chain-owned exception policy around a continuation, without changing its next handler. */
    @FunctionalInterface
    public interface Boundary {
        void handle(HttpServerExchange exchange, HttpHandler continuation) throws Exception;
    }

    /**
     * Runs inside the supplied boundary and restores the enclosing boundary on
     * return. Nested boundaries and continuations can safely capture their scope.
     */
    public static void execute(HttpServerExchange exchange, Boundary boundary,
                               HttpHandler continuation) throws Exception {
        Boundary previous = exchange.putAttachment(ACTIVE, boundary);
        try {
            if (boundary == null) continuation.handleRequest(exchange);
            else boundary.handle(exchange, continuation);
        } finally {
            if (previous == null) exchange.removeAttachment(ACTIVE);
            else exchange.putAttachment(ACTIVE, previous);
        }
    }

    /**
     * Captures the selected boundary, including its deliberate absence. No default
     * exception policy is installed when the chain has none.
     */
    public static HttpHandler capture(HttpServerExchange exchange, HttpHandler continuation) {
        Boundary boundary = exchange.getAttachment(ACTIVE);
        return resumed -> execute(resumed, boundary, continuation);
    }
}
