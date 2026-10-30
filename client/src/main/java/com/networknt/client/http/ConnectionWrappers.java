package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Weak keys AND values avoid rooting the transport/view cycle retained by a native listener. */
final class ConnectionWrappers {
    private static final ReferenceQueue<ClientConnection> collected = new ReferenceQueue<>();
    private static final ConcurrentMap<Key, WeakReference<IoThreadClientConnection>> entries = new ConcurrentHashMap<>();
    private ConnectionWrappers() { }

    static IoThreadClientConnection wrap(ClientConnection raw) {
        Key dead;
        while ((dead = (Key) collected.poll()) != null) entries.remove(dead);
        Key key = new Key(raw, collected);
        for (;;) {
            WeakReference<IoThreadClientConnection> existing = entries.get(key);
            IoThreadClientConnection view = existing == null ? null : existing.get();
            if (view != null) return view;
            IoThreadClientConnection created = new IoThreadClientConnection(raw);
            WeakReference<IoThreadClientConnection> replacement = new WeakReference<>(created);
            if (existing == null ? entries.putIfAbsent(key, replacement) == null : entries.replace(key, existing, replacement)) {
                return created;
            }
        }
    }

    private static final class Key extends WeakReference<ClientConnection> {
        final int hash;
        Key(ClientConnection raw, ReferenceQueue<ClientConnection> queue) { super(raw, queue); hash = System.identityHashCode(raw); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key key && get() != null && get() == key.get();
        }
    }
}
