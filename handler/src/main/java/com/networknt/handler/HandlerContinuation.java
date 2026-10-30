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
    private record Frame(HttpServerExchange exchange, CapturedBoundary boundary) { }
    private static final ThreadLocal<Frame> CURRENT_FRAME = new ThreadLocal<>();

    @FunctionalInterface
    private interface ScopedAction<E extends Exception> { void run() throws E; }

    private record CapturedBoundary(Boundary boundary) implements UnaryOperator<HttpHandler> {
        @Override public HttpHandler apply(HttpHandler continuation) {
            return exchange -> withMdc(exchange, () -> {
                Frame previous = CURRENT_FRAME.get();
                CURRENT_FRAME.set(new Frame(exchange, this));
                try { execute(exchange, boundary, continuation); }
                finally { CURRENT_FRAME.set(previous); }
            });
        }
    }

    // Package-private: proxy callbacks can reuse their exact active policy frame.
    static boolean isActive(HttpServerExchange exchange, UnaryOperator<HttpHandler> boundary) {
        Frame frame = CURRENT_FRAME.get();
        return frame != null && frame.exchange() == exchange && frame.boundary() == boundary;
    }

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
        return new CapturedBoundary(boundary);
    }

    private static <E extends Exception> void withMdc(HttpServerExchange exchange, ScopedAction<E> callback) throws E {
        Frame previousFrame = CURRENT_FRAME.get();
        if (previousFrame != null && previousFrame.exchange() == exchange) {
            callback.run();
            return;
        }
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        Map<String, String> requestMdc;
        synchronized (exchange) {
            requestMdc = exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
        }
        CURRENT_FRAME.set(new Frame(exchange, null));
        try {
            setMdc(requestMdc);
            callback.run();
        } finally {
            setMdc(previousMdc);
            if (previousFrame == null) CURRENT_FRAME.remove();
            else CURRENT_FRAME.set(previousFrame);
        }
    }

    /**
     * Adapts an already captured handler for a Runnable dispatch. Unlike a root
     * handler, a successful callback may leave upstream I/O pending; do not end
     * that exchange. Unhandled failures return 500 before commitment, close a
     * committed stream, or leave an already completed exchange alone.
     */
    public static Runnable task(HttpServerExchange exchange, HttpHandler continuation) {
        return () -> withMdc(exchange, () -> {
            try {
                continuation.handleRequest(exchange);
            } catch (Error fatal) {
                // Best-effort request/stream termination; preserve the original fatal error.
                try {
                    if (!exchange.isComplete() && exchange.getConnection() != null) exchange.getConnection().close();
                } catch (Throwable cleanupFailure) {
                    if (cleanupFailure != fatal) fatal.addSuppressed(cleanupFailure);
                }
                throw fatal;
            } catch (Exception failure) {
                failTask(exchange, failure);
            }
        });
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
