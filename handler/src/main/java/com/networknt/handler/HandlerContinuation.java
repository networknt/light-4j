package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import io.undertow.util.StatusCodes;

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
     * Captures only the exception policy. MDC remains owned by the existing
     * MDC_CONTEXT attachment and its executor; no thread-local maps are copied.
     */
    public static UnaryOperator<HttpHandler> capture(HttpServerExchange exchange) {
        Boundary active = exchange.getAttachment(ACTIVE);
        Boundary boundary = active == null ? UNHANDLED : active;
        return continuation -> resumed -> execute(resumed, boundary, continuation);
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
                continuation.handleRequest(exchange);
            } catch (Throwable failure) {
                LOG.error("Unhandled continuation failure for {}", exchange.getRequestPath(), failure);
                if (exchange.isComplete()) return;
                if (exchange.isResponseStarted()) {
                    // Never replace committed headers or append an error to a stream.
                    IoUtils.safeClose(exchange.getConnection());
                } else {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.endExchange();
                }
            }
        };
    }
}
