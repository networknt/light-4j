package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import org.xnio.IoUtils;
import org.xnio.XnioIoThread;

import java.io.IOException;

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

    /**
     * Send a request on its connection's I/O thread, retaining Undertow's asynchronous callbacks.
     * Dispatch rejection and synchronous send failures are reported through the failure callback.
     * @param connection the connection
     * @param request the request
     * @param callback the exchange callback
     */
    public static void sendRequest(ClientConnection connection, ClientRequest request, ClientCallback<ClientExchange> callback) {
        try {
            execute(connection, () -> {
                try {
                    connection.sendRequest(request, callback);
                } catch (RuntimeException e) {
                    callback.failed(new IOException("Unable to send client request", e));
                }
            });
        } catch (RuntimeException e) {
            callback.failed(new IOException("Unable to dispatch client request", e));
        }
    }

    /**
     * Close without racing pending connection operations on another thread.
     * @param connection the connection to close
     */
    public static void safeClose(ClientConnection connection) {
        execute(connection, () -> IoUtils.safeClose(connection));
    }
}
