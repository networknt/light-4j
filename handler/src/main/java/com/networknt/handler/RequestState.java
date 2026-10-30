package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpServerExchange;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** MDC only: exception policy is carried explicitly by captured scopes. */
final class RequestState {
    private interface Key { HttpServerExchange get(); }

    private static final class WeakKey extends WeakReference<HttpServerExchange> implements Key {
        private final int hash;
        private WeakKey(HttpServerExchange exchange) {
            super(exchange, CLEARED);
            hash = System.identityHashCode(exchange);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            HttpServerExchange exchange = get();
            return this == other || exchange != null && other instanceof Key key && exchange == key.get();
        }
    }

    /** Reusable lookup key; never inserted and never retains an exchange after get(). */
    private static final class LookupKey implements Key {
        private HttpServerExchange exchange;
        @Override public HttpServerExchange get() { return exchange; }
        @Override public int hashCode() { return System.identityHashCode(exchange); }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && exchange == key.get();
        }
    }

    private static final ReferenceQueue<HttpServerExchange> CLEARED = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Key, RequestState> INDEX = new ConcurrentHashMap<>();
    private static final ThreadLocal<LookupKey> LOOKUP = ThreadLocal.withInitial(LookupKey::new);
    // Values contain strings/MDC only, never a boundary closure or an exchange.
    final RequestMdcContext mdc;

    private RequestState(RequestMdcContext mdc) { this.mdc = mdc; }

    static RequestState find(HttpServerExchange exchange) {
        LookupKey key = LOOKUP.get();
        key.exchange = exchange;
        try { return INDEX.get(key); }
        finally { key.exchange = null; }
    }

    static RequestState initialize(HttpServerExchange exchange) { return initialize(exchange, Map.of()); }

    static RequestState initialize(HttpServerExchange exchange, Map<String, String> inheritedMdc) {
        RequestState existing = find(exchange);
        if (existing != null) return existing;
        if (exchange.isComplete()) return detached(exchange, inheritedMdc);
        // Bounded weak-key cleanup; no exchange-completion listener is registered.
        for (int count = 0; count < 16; count++) {
            var key = CLEARED.poll();
            if (key == null) break;
            INDEX.remove(key);
        }
        WeakKey key = new WeakKey(exchange);
        try {
            RequestState state = INDEX.computeIfAbsent(key, ignored -> {
                RequestState created = detached(exchange, inheritedMdc);
                exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, created.mdc);
                return created;
            });
            return state;
        } finally { Reference.reachabilityFence(exchange); }
    }

    private static RequestState detached(HttpServerExchange exchange, Map<String, String> inheritedMdc) {
        Map<String, String> attached = exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
        Map<String, String> initial = attached == null ? inheritedMdc : attached;
        RequestMdcContext context = attached instanceof RequestMdcContext holder ? holder : new RequestMdcContext(initial);
        return new RequestState(context);
    }
}
