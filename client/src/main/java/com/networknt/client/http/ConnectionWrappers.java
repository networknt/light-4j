package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import java.lang.ref.Cleaner;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Weak transport keys and views avoid rooting transports; retirement state outlives collected views. */
final class ConnectionWrappers {
    private static final ConnectionWrappers SHARED = new ConnectionWrappers(Cleaner.create(), new ConcurrentHashMap<>());
    private final Cleaner cleaner;
    private final ConcurrentMap<WeakReference<ClientConnection>, Entry> entries;

    ConnectionWrappers(Cleaner cleaner, ConcurrentMap<WeakReference<ClientConnection>, Entry> entries) {
        this.cleaner = cleaner;
        this.entries = entries;
    }
    static IoThreadClientConnection wrap(ClientConnection raw) { return SHARED.getOrCreate(raw); }
    static IoThreadClientConnection find(ClientConnection connection) { return SHARED.lookup(connection); }

    IoThreadClientConnection getOrCreate(ClientConnection raw) {
        Entry entry = entries.computeIfAbsent(new Key(raw, entries), key -> {
            // The registered action is the stored weak key, never a closure capturing raw or a view.
            cleaner.register(raw, (Key) key);
            return new Entry();
        });
        return entry.getOrCreateView(raw);
    }
    // Lookup does not create a view, register listeners, or touch the raw transport.
    IoThreadClientConnection lookup(ClientConnection connection) {
        if (connection instanceof IoThreadClientConnection owned) return owned;
        Entry entry = entries.get(new Key(connection, entries));
        return entry == null ? null : entry.view.get().get();
    }

    static final class Entry {
        private Entry() { }
        final AtomicBoolean closed = new AtomicBoolean();
        volatile IOException closeReason;
        final AtomicReference<WeakReference<IoThreadClientConnection>> view = new AtomicReference<>(new WeakReference<>(null));
        IoThreadClientConnection getOrCreateView(ClientConnection raw) {
            for (;;) {
                WeakReference<IoThreadClientConnection> existing = view.get();
                IoThreadClientConnection current = existing.get();
                if (current != null) return current;
                IoThreadClientConnection created = new IoThreadClientConnection(raw, this);
                if (view.compareAndSet(existing, new WeakReference<>(created))) return created;
            }
        }
    }

    private static final class Key extends WeakReference<ClientConnection> implements Runnable {
        private final int hash;
        private final ConcurrentMap<WeakReference<ClientConnection>, Entry> owner;
        Key(ClientConnection raw, ConcurrentMap<WeakReference<ClientConnection>, Entry> owner) {
            super(raw);
            hash = System.identityHashCode(raw);
            this.owner = owner;
        }
        @Override public void run() { owner.remove(this); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            ClientConnection raw = get();
            return raw != null && other instanceof Key key && raw == key.get();
        }
    }
}
