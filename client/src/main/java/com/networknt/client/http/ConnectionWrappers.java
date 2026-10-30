package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Weak identity keys and weak views avoid retaining otherwise unreachable transports. */
final class ConnectionWrappers {
    private static final ReferenceQueue<ClientConnection> collected = new ReferenceQueue<>();
    private static final Map<Key, Entry> entries = new HashMap<>();
    private ConnectionWrappers() { }

    static synchronized IoThreadClientConnection wrap(ClientConnection raw) {
        Key dead;
        while ((dead = (Key) collected.poll()) != null) entries.remove(dead);
        Key lookup = new Key(raw, null);
        Entry entry = entries.get(lookup);
        if (entry == null) {
            entry = new Entry();
            entries.put(new Key(raw, collected), entry);
        }
        IoThreadClientConnection view = entry.view.get();
        if (view == null) {
            view = new IoThreadClientConnection(raw, entry);
            entry.view = new WeakReference<>(view);
        }
        return view;
    }

    static final class Entry {
        // Retirement persists while the raw transport is reachable, even if its view is collected.
        final AtomicBoolean closed = new AtomicBoolean();
        volatile java.io.IOException closeReason;
        WeakReference<IoThreadClientConnection> view = new WeakReference<>(null);
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
