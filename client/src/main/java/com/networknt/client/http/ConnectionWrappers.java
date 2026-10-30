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
    private static final Cleaner CLEANER = Cleaner.create();
    private static final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private ConnectionWrappers() { }

    static IoThreadClientConnection wrap(ClientConnection raw) {
        Entry entry = entries.computeIfAbsent(new Key(raw), key -> {
            // Cleaner retains only the weak identity key; neither the transport nor its view.
            CLEANER.register(raw, key);
            return new Entry();
        });
        return entry.getOrCreateView(raw);
    }

    // Lookup does not create a view, register listeners, or touch the raw transport.
    static IoThreadClientConnection find(ClientConnection connection) {
        if (connection instanceof IoThreadClientConnection owned) return owned;
        Entry entry = entries.get(new Key(connection));
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
        final int hash;
        Key(ClientConnection raw) { super(raw); hash = System.identityHashCode(raw); }
        @Override public void run() { entries.remove(this); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key key && get() != null && get() == key.get();
        }
    }
}
