package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Carries the active exception boundary across an asynchronous hand-off. Boundary
 * policy is captured from the executing frame, never stored on shared middleware.
 * Capture before returning from the current chain frame; invoke the captured
 * handler through Undertow's dispatch or root-handler machinery.
 */
public final class HandlerContinuation {
    private static final Logger LOG = LoggerFactory.getLogger(HandlerContinuation.class);
    private static final Boundary UNHANDLED = (exchange, continuation) -> continuation.handleRequest(exchange);
    private record Frame(HttpServerExchange exchange, CapturedBoundary boundary, boolean mdcScoped) { }
    private static final ThreadLocal<Frame> CURRENT_FRAME = new ThreadLocal<>();

    @FunctionalInterface
    private interface ScopedAction<E extends Exception> { void run() throws E; }

    private record CapturedBoundary(Boundary boundary, RequestMdcContext context) implements UnaryOperator<HttpHandler> {
        @Override public HttpHandler apply(HttpHandler continuation) {
            return new CapturedHandler(this, continuation);
        }
    }

    private record CapturedHandler(CapturedBoundary policy, HttpHandler continuation) implements HttpHandler {
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            withMdc(exchange, policy, () -> policy.boundary().handle(exchange, continuation));
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
        Objects.requireNonNull(boundary);
        Frame previous = CURRENT_FRAME.get();
        RequestMdcContext context = context(exchange);
        CURRENT_FRAME.set(new Frame(exchange, new CapturedBoundary(boundary, context),
                previous != null && previous.exchange() == exchange && previous.mdcScoped()));
        try { boundary.handle(exchange, continuation); }
        finally { restoreFrame(previous); }
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
     * Captures the exception policy and the stable request MDC holder. At resume,
     * MDC comes from its current immutable generation, not a thread-local snapshot. The
     * executing thread's previous MDC is restored even on failure. Nested frames
     * for the same exchange reuse the active scope.
     */
    public static UnaryOperator<HttpHandler> capture(HttpServerExchange exchange) {
        Frame frame = CURRENT_FRAME.get();
        Boundary boundary = frame != null && frame.exchange() == exchange ? frame.boundary().boundary() : UNHANDLED;
        return new CapturedBoundary(boundary, context(exchange));
    }

    // Resume paths use the captured holder, never the exchange attachment map.
    static RequestMdcContext activeContext(HttpServerExchange exchange) {
        Frame frame = CURRENT_FRAME.get();
        return frame != null && frame.exchange() == exchange ? frame.boundary().context() : null;
    }

    private static RequestMdcContext context(HttpServerExchange exchange) {
        RequestMdcContext active = activeContext(exchange);
        return active != null ? active : RequestMdcContext.initialize(exchange);
    }

    private static <E extends Exception> void withMdc(HttpServerExchange exchange, CapturedBoundary policy,
                                                      ScopedAction<E> callback) throws E {
        Frame previousFrame = CURRENT_FRAME.get();
        boolean reuseMdc = previousFrame != null && previousFrame.exchange() == exchange && previousFrame.mdcScoped();
        if (reuseMdc && previousFrame.boundary() == policy) {
            callback.run();
            return;
        }
        Map<String, String> previousMdc = reuseMdc ? null : MDC.getCopyOfContextMap();
        CURRENT_FRAME.set(new Frame(exchange, policy, true));
        try {
            if (!reuseMdc) {
                Map<String, String> requestMdc = policy.context().snapshot();
                if (!requestMdc.equals(previousMdc == null ? Map.of() : previousMdc)) setMdc(requestMdc);
            }
            callback.run();
        } finally {
            if (!reuseMdc) setMdc(previousMdc);
            restoreFrame(previousFrame);
        }
    }

    private static void restoreFrame(Frame previous) {
        if (previous == null) CURRENT_FRAME.remove();
        else CURRENT_FRAME.set(previous);
    }

    /**
     * Adapts an already captured handler for a Runnable dispatch. Unlike a root
     * handler, a successful callback may leave upstream I/O pending; do not end
     * that exchange. Unhandled failures return 500 before commitment, close a
     * committed stream, or leave an already completed exchange alone.
     */
    @SuppressWarnings("java:S1181") // Fatal errors require best-effort request cleanup before rethrow.
    public static Runnable task(HttpServerExchange exchange, HttpHandler continuation) {
        HttpHandler captured = continuation instanceof CapturedHandler ? continuation : capture(exchange, continuation);
        return () -> {
            try { captured.handleRequest(exchange); }
            catch (Error fatal) {
                finishFailedRequest(exchange, fatal);
                throw fatal;
            } catch (Exception failure) {
                finishFailedRequest(exchange, failure);
                LOG.error("Unhandled continuation failure for {}", exchange.getRequestPath(), failure);
            }
        };
    }

    /** Preserve the original failure even if the response or stream cleanup fails. */
    @SuppressWarnings("java:S1181") // Error cleanup must not mask the original VM/application Error.
    private static void finishFailedRequest(HttpServerExchange exchange, Throwable failure) {
        if (exchange.isComplete()) return;
        if (!exchange.isResponseStarted()) {
            try {
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.endExchange();
                return;
            } catch (Throwable writeFailure) { rememberCleanupFailure(failure, writeFailure); }
        }
        try {
            // HTTP/2 ServerConnection.close() resets only this stream.
            if (exchange.getConnection() != null) exchange.getConnection().close();
        } catch (Throwable closeFailure) { rememberCleanupFailure(failure, closeFailure); }
    }

    @SuppressWarnings("java:S1181") // Suppression metadata can itself fail under memory pressure.
    private static void rememberCleanupFailure(Throwable failure, Throwable cleanupFailure) {
        if (cleanupFailure == failure) return;
        try { failure.addSuppressed(cleanupFailure); }
        catch (Throwable ignored) {
            // Preserve the original failure even when suppression metadata cannot be allocated.
        }
    }

    private static void setMdc(Map<String, String> context) {
        if (context == null || context.isEmpty()) MDC.clear();
        else MDC.setContextMap(context);
    }
}
