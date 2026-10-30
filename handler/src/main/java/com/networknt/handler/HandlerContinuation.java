package com.networknt.handler;

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
            return new CapturedHandler(this, continuation);
        }
    }

    private record CapturedHandler(CapturedBoundary policy, HttpHandler continuation) implements HttpHandler {
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            withMdc(exchange, policy, () -> execute(exchange, policy.boundary(), continuation));
        }
    }

    /** True only while this exact captured policy is executing for this exchange. */
    public static boolean isActive(HttpServerExchange exchange, UnaryOperator<HttpHandler> boundary) {
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
        RequestMdcContext.initialize(exchange);
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
        RequestMdcContext.initialize(exchange);
        Boundary active = exchange.getAttachment(ACTIVE);
        Boundary boundary = active == null ? UNHANDLED : active;
        return new CapturedBoundary(boundary);
    }

    private static <E extends Exception> void withMdc(HttpServerExchange exchange, CapturedBoundary policy,
                                                      ScopedAction<E> callback) throws E {
        Frame previousFrame = CURRENT_FRAME.get();
        if (previousFrame != null && previousFrame.exchange() == exchange) {
            if (policy == null || previousFrame.boundary() == policy) callback.run();
            else {
                CURRENT_FRAME.set(new Frame(exchange, policy));
                try { callback.run(); }
                finally { CURRENT_FRAME.set(previousFrame); }
            }
            return;
        }
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        Map<String, String> requestMdc = RequestMdcContext.read(exchange);
        CURRENT_FRAME.set(new Frame(exchange, policy));
        try {
            if (!requestMdc.equals(previousMdc == null ? Map.of() : previousMdc)) setMdc(requestMdc);
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
        if (continuation instanceof CapturedHandler captured) {
            HttpHandler action = ex -> execute(ex, captured.policy().boundary(), captured.continuation());
            return () -> withMdc(exchange, captured.policy(), () -> invokeTask(exchange, action));
        }
        return () -> withMdc(exchange, null, () -> invokeTask(exchange, continuation));
    }

    private static void invokeTask(HttpServerExchange exchange, HttpHandler continuation) {
        try {
            continuation.handleRequest(exchange);
        } catch (Error fatal) {
            failFatal(exchange, fatal);
            throw fatal;
        } catch (Exception failure) {
            failTask(exchange, failure);
        }
    }

    private static void failFatal(HttpServerExchange exchange, Error fatal) {
        if (exchange.isComplete()) return;
        if (exchange.isResponseStarted()) {
            closeAfterFatal(exchange, fatal);
            return;
        }
        try {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
        } catch (Throwable writeFailure) {
            rememberCleanupFailure(fatal, writeFailure);
            closeAfterFatal(exchange, fatal);
        }
    }

    private static void closeAfterFatal(HttpServerExchange exchange, Error fatal) {
        try {
            if (exchange.getConnection() != null) exchange.getConnection().close();
        } catch (Throwable closeFailure) {
            rememberCleanupFailure(fatal, closeFailure);
        }
    }

    private static void rememberCleanupFailure(Error fatal, Throwable cleanupFailure) {
        if (cleanupFailure == fatal) return;
        try { fatal.addSuppressed(cleanupFailure); }
        catch (Throwable ignored) {
            // Preserve the original Error even if the VM cannot allocate suppression metadata.
        }
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
