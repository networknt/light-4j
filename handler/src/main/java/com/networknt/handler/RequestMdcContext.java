package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpServerExchange;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Stable, read-only Map view of request MDC. Initialize in the first serial
 * request frame before handing the exchange to another thread. Subsequent
 * publication changes only the atomic snapshot, never the attachment map.
 */
public final class RequestMdcContext extends AbstractMap<String, String> {
    private final AtomicReference<Map<String, String>> current;

    private RequestMdcContext(Map<String, String> initial) {
        current = new AtomicReference<>(initial == null || initial.isEmpty()
                ? Map.of() : Collections.unmodifiableMap(new HashMap<>(initial)));
    }

    /**
     * First use must be serial. Boundary execution and capture initialize before
     * registering asynchronous work; repeated calls do not write any attachment.
     * Legacy Map-valued context is migrated once in that serial frame.
     */
    public static RequestMdcContext initialize(HttpServerExchange exchange) {
        RequestMdcContext active = HandlerContinuation.activeContext(exchange);
        if (active != null) return active;
        Map<String, String> attached = exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
        if (attached instanceof RequestMdcContext holder) return holder;
        RequestMdcContext holder = new RequestMdcContext(attached);
        exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, holder);
        return holder;
    }

    /** One immutable generation suitable for coherent iteration by external readers. */
    public Map<String, String> snapshot() { return current.get(); }

    void putField(String field, String value) {
        current.updateAndGet(previous -> {
            Map<String, String> next = new HashMap<>(previous);
            next.put(field, value);
            return Collections.unmodifiableMap(next);
        });
    }

    @Override public String get(Object key) { return current.get().get(key); }
    @Override public boolean containsKey(Object key) { return current.get().containsKey(key); }
    @Override public boolean equals(Object other) { return other == this || current.get().equals(other); }
    @Override public int hashCode() { return current.get().hashCode(); }
    @Override public int size() { return current.get().size(); }
    @Override public Set<Entry<String, String>> entrySet() { return current.get().entrySet(); }
    @Override public void forEach(BiConsumer<? super String, ? super String> consumer) {
        current.get().forEach(consumer);
    }
}
