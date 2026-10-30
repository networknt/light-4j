package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import java.lang.ref.ReferenceQueue;
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
    private static final ReferenceQueue<ClientConnection> collected = new ReferenceQueue<>();
    private static final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private ConnectionWrappers() { }

    static IoThreadClientConnection wrap(ClientConnection raw) {
        purgeCollectedKeys();
        Entry entry = entries.computeIfAbsent(new Key(raw, collected), key -> new Entry(raw, key));
        return entry.getOrCreateView(raw);
    }

    // Lookup does not create a view, register listeners, or touch the raw transport.
    static IoThreadClientConnection find(ClientConnection connection) {
        if (connection instanceof IoThreadClientConnection owned) return owned;
        Entry entry = lookup(connection);
        return entry == null ? null : entry.view.get().get();
    }

    static Entry lookup(ClientConnection raw) { return entries.get(new Key(raw, null)); }

    static void purgeCollectedKeys() {
        Key dead;
        while ((dead = (Key) collected.poll()) != null) entries.remove(dead);
    }

    private static void removeCollectedKey(Key key) {
        entries.remove(key);
        purgeCollectedKeys();
    }

    static final class Entry {
        // Observational only: check registry cleanup without triggering it.
        boolean isRegistered() { return entries.get(key) == this; }
        final WeakReference<ClientConnection> key;
        final Cleaner.Cleanable cleanable;
        private Entry(ClientConnection raw, Key key) {
            this.key = key;
            // The action captures only a weak key, never raw, the view, or this entry.
            cleanable = CLEANER.register(raw, () -> removeCollectedKey(key));
        }
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

    private static final class Key extends WeakReference<ClientConnection> {
        final int hash;
        Key(ClientConnection raw, ReferenceQueue<ClientConnection> queue) { super(raw, queue); hash = System.identityHashCode(raw); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key key && get() != null && get() == key.get();
        }
    }
}
