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
    static final ConnectionWrappers INSTANCE = new ConnectionWrappers(Cleaner.create());
    private final Cleaner cleaner;
    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();

    ConnectionWrappers(Cleaner cleaner) { this.cleaner = cleaner; }

    IoThreadClientConnection getOrCreate(ClientConnection raw) {
        Entry entry = entries.computeIfAbsent(new Key(raw), key -> {
            cleaner.register(raw, new Cleanup(entries, key));
            return new Entry();
        });
        return entry.getOrCreateView(raw);
    }
    // Lookup does not create a view, register listeners, or touch the raw transport.
    Entry lookup(ClientConnection connection) {
        return entries.get(new Key(connection));
    }

    static final class Cleanup implements Runnable {
        private final ConcurrentMap<Key, Entry> records;
        private final Key key;
        private Cleanup(ConcurrentMap<Key, Entry> records, Key key) {
            this.records = records;
            this.key = key;
        }
        @Override public void run() {
            key.clear();
            records.remove(key);
        }
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

    private static final class Key extends WeakReference<ClientConnection> {
        private final int hash;
        Key(ClientConnection raw) {
            super(raw);
            hash = System.identityHashCode(raw);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            ClientConnection raw = get();
            return raw != null && other instanceof Key key && raw == key.get();
        }
    }
}
