package com.networknt.client.http;

import io.undertow.client.ClientConnection;
import org.xnio.ChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local registration is synchronous; all user callouts happen outside the listener monitor. */
final class ConnectionCloseListeners {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionCloseListeners.class);
    private final ClientConnection connection;
    private final List<ChannelListener<? super ClientConnection>> listeners = new ArrayList<>();
    private ChannelListener<? super ClientConnection> setter;
    private boolean notified;
    ConnectionCloseListeners(ClientConnection connection) { this.connection = connection; }

    void set(ChannelListener<? super ClientConnection> listener) {
        synchronized (this) {
            if (!notified) { setter = listener; return; }
        }
        if (listener != null) invoke(listener);
    }
    void add(ChannelListener<? super ClientConnection> listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (this) {
            if (!notified) { listeners.add(listener); return; }
        }
        invoke(listener);
    }
    void fire() {
        List<ChannelListener<? super ClientConnection>> snapshot;
        synchronized (this) {
            if (notified) return;
            notified = true;
            snapshot = new ArrayList<>(listeners);
            if (setter != null) snapshot.add(setter);
            listeners.clear();
            setter = null;
        }
        for (ChannelListener<? super ClientConnection> listener : snapshot) invoke(listener);
    }
    private void invoke(ChannelListener<? super ClientConnection> listener) {
        ClientConnectionOperations.runWithFailureHandler(() -> listener.handleEvent(connection),
                error -> logger.error("Client close listener failed", error));
    }
}
