package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
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
    private static final Logger LOG = LoggerFactory.getLogger(HandlerContinuation.class);
    private static final Boundary UNHANDLED = (exchange, continuation) -> continuation.handleRequest(exchange);
    private static final AttachmentKey<Boundary> ACTIVE = AttachmentKey.create(Boundary.class);
    private static final ThreadLocal<HttpServerExchange> MDC_EXCHANGE = new ThreadLocal<>();

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
     * Captures only the exception policy. At resume, MDC comes from the canonical
     * MDC_CONTEXT attachment, not from a captured thread-local snapshot. The
     * executing thread's previous MDC is restored even on failure. Nested frames
     * for the same exchange reuse the active scope.
     */
    public static UnaryOperator<HttpHandler> capture(HttpServerExchange exchange) {
        Boundary active = exchange.getAttachment(ACTIVE);
        Boundary boundary = active == null ? UNHANDLED : active;
        return continuation -> resumed -> withMdc(resumed,
                ex -> execute(ex, boundary, continuation));
    }

    private static void withMdc(HttpServerExchange exchange, HttpHandler callback) throws Exception {
        if (MDC_EXCHANGE.get() == exchange) {
            callback.handleRequest(exchange);
            return;
        }
        HttpServerExchange previousExchange = MDC_EXCHANGE.get();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        Map<String, String> requestMdc = exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
        MDC_EXCHANGE.set(exchange);
        try {
            setMdc(requestMdc);
            callback.handleRequest(exchange);
        } finally {
            setMdc(previousMdc);
            if (previousExchange == null) MDC_EXCHANGE.remove();
            else MDC_EXCHANGE.set(previousExchange);
        }
    }

    /**
     * Adapts an already captured handler for a Runnable dispatch. Unlike a root
     * handler, a successful callback may leave upstream I/O pending; do not end
     * that exchange. Unhandled failures return 500 before commitment, close a
     * committed stream, or leave an already completed exchange alone.
     */
    public static Runnable task(HttpServerExchange exchange, HttpHandler continuation) {
        return () -> {
            try {
                withMdc(exchange, ex -> {
                    try {
                        continuation.handleRequest(ex);
                    } catch (Exception failure) {
                        failTask(ex, failure);
                    }
                });
            } catch (RuntimeException failure) {
                throw failure;
            } catch (Exception impossible) {
                throw new IllegalStateException(impossible);
            }
        };
    }

    private static void failTask(HttpServerExchange exchange, Exception failure) {
        LOG.error("Unhandled continuation failure for {}", exchange.getRequestPath(), failure);
        if (exchange.isComplete()) return;
        if (exchange.isResponseStarted()) {
            // Never replace committed headers or append an error to a stream.
            try {
                // HTTP/2 ServerConnection.close() resets this stream only.
                exchange.getConnection().close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
                LOG.debug("Unable to close failed response stream", closeFailure);
            }
        } else {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
        }
    }

    private static void setMdc(Map<String, String> context) {
        if (context == null || context.isEmpty()) MDC.clear();
        else MDC.setContextMap(context);
    }
}
