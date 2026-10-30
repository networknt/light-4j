package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/** Exchange-owned state with an independent lookup for callbacks on other threads. */
final class RequestState {
    private static final AttachmentKey<RequestState> KEY = AttachmentKey.create(RequestState.class);
    // Weak keys AND values: a user boundary may itself reference its exchange.
    // The exchange attachment, rather than this index, owns the state's lifetime.
    private static final Map<HttpServerExchange, WeakReference<RequestState>> INDEX = new WeakHashMap<>();
    final RequestMdcContext mdc;
    private final AtomicReference<UnaryOperator<HttpHandler>> entryBoundary = new AtomicReference<>();

    private RequestState(RequestMdcContext mdc) { this.mdc = mdc; }

    static RequestState initialize(HttpServerExchange exchange) { return initialize(exchange, Map.of()); }

    static RequestState initialize(HttpServerExchange exchange, Map<String, String> inheritedMdc) {
        synchronized (INDEX) {
            WeakReference<RequestState> reference = INDEX.get(exchange);
            RequestState existing = reference == null ? null : reference.get();
            if (existing != null) return existing;
            // First use is serial, before hand-off. Later lookups never touch the
            // unsynchronized attachment map, including off-frame helper calls.
            Map<String, String> attached = exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
            Map<String, String> initialMdc = attached == null ? inheritedMdc : attached;
            RequestMdcContext mdc = attached instanceof RequestMdcContext holder ? holder
                    : new RequestMdcContext(initialMdc);
            RequestState state = new RequestState(mdc);
            exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, mdc);
            exchange.putAttachment(KEY, state);
            INDEX.put(exchange, new WeakReference<>(state));
            return state;
        }
    }

    void rememberBoundary(UnaryOperator<HttpHandler> boundary) { entryBoundary.compareAndSet(null, boundary); }

    UnaryOperator<HttpHandler> boundary() {
        UnaryOperator<HttpHandler> boundary = entryBoundary.get();
        return boundary == null ? UnaryOperator.identity() : boundary;
    }
}
