package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.Connectors;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Carries the active exception boundary across an asynchronous hand-off. Boundary
 * state belongs to an exchange, never a thread or a shared middleware instance.
 * Capture before returning from the current chain frame; invoke the captured
 * handler through Undertow's dispatch or root-handler machinery.
 */
public final class HandlerContinuation {
    private static final Boundary UNHANDLED = (exchange, continuation) -> continuation.handleRequest(exchange);
    private static final AttachmentKey<Boundary> ACTIVE = AttachmentKey.create(Boundary.class);

    private HandlerContinuation() { }

    /**
     * A chain-owned exception policy, without changing its next handler. Invoke
     * the supplied continuation exactly once; handle or rethrow its failure.
     * Implementations must be safe to reuse concurrently across exchanges.
     */
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
        Boundary previous = exchange.putAttachment(ACTIVE, Objects.requireNonNull(boundary));
        try {
            boundary.handle(exchange, continuation);
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
        return capture(exchange).apply(continuation);
    }

    /**
     * Captures before registering a callback whose result determines its eventual
     * work. Apply the returned wrapper inside that callback. Framework hand-offs
     * must opt in; arbitrary third-party callbacks cannot be intercepted here.
     * The request MDC is installed only for the resumed frame and the executing
     * thread's previous MDC is restored, including on failure.
     */
    public static UnaryOperator<HttpHandler> capture(HttpServerExchange exchange) {
        Boundary active = exchange.getAttachment(ACTIVE);
        Boundary boundary = active == null ? UNHANDLED : active;
        Map<String, String> context = MDC.getCopyOfContextMap();
        return continuation -> resumed -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            restoreMdc(context);
            try {
                execute(resumed, boundary, continuation);
            } finally {
                restoreMdc(previous);
            }
        };
    }

    /**
     * Adapts an already captured handler for a Runnable dispatch. Unlike a root
     * handler, a successful callback may leave upstream I/O pending; do not end
     * that exchange. Unhandled failures still enter Undertow's error path.
     */
    public static Runnable task(HttpServerExchange exchange, HttpHandler continuation) {
        return () -> {
            try {
                continuation.handleRequest(exchange);
            } catch (Throwable failure) {
                Connectors.executeRootHandler(ex -> {
                    if (failure instanceof Error error) throw error;
                    if (failure instanceof Exception exception) throw exception;
                    throw new IllegalStateException(failure);
                }, exchange);
            }
        };
    }

    private static void restoreMdc(Map<String, String> context) {
        if (context == null) MDC.clear();
        else MDC.setContextMap(context);
    }
}
