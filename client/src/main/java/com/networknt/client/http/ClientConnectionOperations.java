package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import org.xnio.XnioIoThread;

/** Operations on Undertow connections must run on the connection's owning I/O thread. */
public final class ClientConnectionOperations {
    private ClientConnectionOperations() { }

    /**
     * Execute inline on the owning thread, or enqueue without blocking the caller.
     * @param connection the connection whose I/O thread owns the operation
     * @param operation the nonblocking connection operation
     */
    public static void execute(ClientConnection connection, Runnable operation) {
        XnioIoThread owner = connection.getIoThread();
        if (Thread.currentThread() == owner) operation.run();
        else owner.execute(operation);
    }

    /** Adapt a provider callback at the connection boundary. */
    public static ClientCallback<ClientConnection> wrap(ClientCallback<ClientConnection> callback) {
        return new ClientCallback<>() {
            @Override public void completed(ClientConnection connection) { callback.completed(IoThreadClientConnection.wrap(connection)); }
            @Override public void failed(java.io.IOException error) { callback.failed(error); }
        };
    }

}
