package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import java.lang.ref.ReferenceQueue;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Weak transport keys and views avoid rooting transports; retirement state outlives collected views. */
final class ConnectionWrappers {
    private static final ReferenceQueue<ClientConnection> collected = new ReferenceQueue<>();
    private static final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private ConnectionWrappers() { }

    static IoThreadClientConnection wrap(ClientConnection raw) {
        Key dead;
        while ((dead = (Key) collected.poll()) != null) entries.remove(dead);
        Entry entry = entries.computeIfAbsent(new Key(raw, collected), ignored -> new Entry());
        for (;;) {
            WeakReference<IoThreadClientConnection> existing = entry.view.get();
            IoThreadClientConnection view = existing.get();
            if (view != null) return view;
            IoThreadClientConnection created = new IoThreadClientConnection(raw, entry);
            if (entry.view.compareAndSet(existing, new WeakReference<>(created))) return created;
        }
    }

    // Lookup does not create a view, register listeners, or touch the raw transport.
    static IoThreadClientConnection find(ClientConnection connection) {
        if (connection instanceof IoThreadClientConnection owned) return owned;
        Entry entry = entries.get(new Key(connection, null));
        return entry == null ? null : entry.view.get().get();
    }

    static final class Entry {
        final AtomicBoolean closed = new AtomicBoolean();
        volatile IOException closeReason;
        final AtomicReference<WeakReference<IoThreadClientConnection>> view = new AtomicReference<>(new WeakReference<>(null));
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
