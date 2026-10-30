package com.networknt.handler;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;

/** Lexical exception policy, carried explicitly across asynchronous hand-offs. */
public final class HandlerContinuation {
    private static final Logger LOG = LoggerFactory.getLogger(HandlerContinuation.class);
    private record Frame(HttpServerExchange exchange, RequestState state,
                         Scope scope, boolean mdcScoped) { }
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private HandlerContinuation() { }

    /** Execute within a native Undertow wrapper; restore the enclosing lexical scope on exit. */
    public static void execute(HttpServerExchange exchange, HandlerWrapper boundary, HttpHandler continuation) throws Exception {
        Objects.requireNonNull(boundary);
        Frame previous = CURRENT.get();
        boolean sameExchange = previous != null && previous.exchange() == exchange;
        RequestState state = sameExchange ? previous.state() : RequestState.initialize(exchange);
        Scope scope = new Scope(exchange, boundary, state);
        Frame entry = new Frame(exchange, state, scope, sameExchange && previous.mdcScoped());
        CURRENT.set(entry);
        try { boundary.wrap(new EntryContinuation(scope, continuation, entry)).handleRequest(exchange); }
        finally { restore(previous); }
    }

    /**
     * Capture the current lexical policy. Outside a frame the policy is absent:
     * an exchange cannot identify a caller's position in the chain. Off-thread
     * callers must retain the Scope captured before their hand-off.
     */
    public static Scope capture(HttpServerExchange exchange) {
        Frame frame = CURRENT.get();
        boolean sameExchange = frame != null && frame.exchange() == exchange;
        return sameExchange ? frame.scope() : new Scope(exchange, null, RequestState.initialize(exchange));
    }

    public static HttpHandler capture(HttpServerExchange exchange, HttpHandler continuation) {
        return capture(exchange).wrap(continuation);
    }

    public static Runnable task(HttpServerExchange exchange, HttpHandler continuation) {
        if (continuation instanceof BoundHandler bound) {
            if (exchange != bound.scope.exchange) throw new IllegalArgumentException("Fork the captured scope for a different exchange");
            return bound.scope.task(bound.continuation);
        }
        return capture(exchange).task(continuation);
    }

    /** A reusable, typed request scope. It never guesses a policy from exchange history. */
    public static final class Scope implements HandlerWrapper {
        private final HttpServerExchange exchange;
        private final HandlerWrapper boundary;
        private final RequestState state;
        private final Frame resumedFrame;

        private Scope(HttpServerExchange exchange, HandlerWrapper boundary, RequestState state) {
            this.exchange = exchange;
            this.boundary = boundary;
            this.state = state;
            this.resumedFrame = new Frame(exchange, state, this, true);
        }

        public Scope fork(HttpServerExchange child) {
            return new Scope(child, boundary, RequestState.initialize(child, state.mdc.snapshot()));
        }

        @Override public HttpHandler wrap(HttpHandler continuation) { return new BoundHandler(this, continuation); }

        public Runnable task(HttpHandler continuation) { return () -> execute(continuation); }

        /** Selected policy handles ordinary failures; unhandled Errors are cleaned up then propagated. */
        public void execute(HttpHandler continuation) {
            try {
                inContext(continuation, true);
            } catch (Exception failure) {
                finishFailedRequest(failure);
                LOG.error("Unhandled continuation failure for {}", exchange.getRequestPath(), failure);
            } catch (Error failure) {
                finishFailedRequest(failure);
                throw failure;
            }
        }

        /** Resume accepted work; replay only a failure through the selected policy, never the work itself. */
        public void resume(HttpHandler continuation) {
            try { inContext(continuation, false); }
            catch (Exception | Error failure) { execute(new FailureHandler(failure)); }
        }

        private void inContext(HttpHandler continuation, boolean enterBoundary) throws Exception {
            Frame previous = CURRENT.get();
            boolean reuse = previous != null && previous.exchange() == exchange && previous.mdcScoped();
            boolean same = reuse && previous.scope() == this;
            Map<String, String> before = reuse ? null : MDC.getCopyOfContextMap();
            if (!same) CURRENT.set(resumedFrame);
            try {
                if (!reuse) {
                    Map<String, String> request = state.mdc.snapshot();
                    if (!request.equals(before == null ? Map.of() : before)) setMdc(request);
                }
                invoke(continuation, enterBoundary);
            } finally {
                if (!reuse) setMdc(before);
                if (!same) restore(previous);
            }
        }

        private void invoke(HttpHandler continuation, boolean enterBoundary) throws Exception {
            if (!enterBoundary || boundary == null) {
                continuation.handleRequest(exchange);
                return;
            }
            HttpHandler contextual = continuation instanceof ScopedCallback callback && callback.scope() == this
                    ? continuation : new ContextualHandler(this, continuation);
            boundary.wrap(contextual).handleRequest(exchange);
        }

        private static void setMdc(Map<String, String> context) {
            if (context == null || context.isEmpty()) MDC.clear();
            else MDC.setContextMap(context);
        }

        /** Best-effort cleanup must never mask an already propagating failure. */
        private void finishFailedRequest(Throwable primary) {
            if (exchange.isComplete()) return;
            if (!exchange.isResponseStarted()) {
                try {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.endExchange();
                    return;
                } catch (Throwable cleanupFailure) {
                    rememberCleanupFailure(primary, cleanupFailure);
                }
            }
            try {
                if (exchange.getConnection() != null) exchange.getConnection().close();
            } catch (Throwable cleanupFailure) {
                rememberCleanupFailure(primary, cleanupFailure);
            }
        }
        private static void rememberCleanupFailure(Throwable primary, Throwable cleanupFailure) {
            if (primary == cleanupFailure) return;
            try {
                primary.addSuppressed(cleanupFailure);
                LOG.error("Continuation cleanup failed; retaining the original failure", cleanupFailure);
            } catch (Throwable _) {
                // Diagnostics can fail under memory pressure; never replace the primary failure.
            }
        }

    }

    private interface ScopedCallback extends HttpHandler { Scope scope(); }

    private record EntryContinuation(Scope scope, HttpHandler continuation, Frame entry) implements HttpHandler {
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (CURRENT.get() == entry) continuation.handleRequest(exchange);
            else scope.inContext(continuation, false);
        }
    }

    private record BoundHandler(Scope scope, HttpHandler continuation, ContextualHandler contextual) implements HttpHandler {
        private BoundHandler(Scope scope, HttpHandler continuation) {
            this(scope, continuation, new ContextualHandler(scope, continuation));
        }
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange != scope.exchange) throw new IllegalArgumentException("Fork the captured scope for a different exchange");
            scope.inContext(contextual, true);
        }
    }

    private record ContextualHandler(Scope scope, HttpHandler continuation) implements ScopedCallback {
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange != scope.exchange) throw new IllegalArgumentException("Fork the captured scope for a different exchange");
            scope.inContext(continuation, false);
        }
    }

    private record FailureHandler(Throwable failure) implements HttpHandler {
        @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new IllegalArgumentException("Unsupported failure", failure);
        }
    }

    private static void restore(Frame frame) {
        if (frame == null) CURRENT.remove();
        else CURRENT.set(frame);
    }


}
