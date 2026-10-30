package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import org.xnio.XnioIoThread;
import java.io.IOException;
import java.util.function.Consumer;

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

    /** An operation that can fail while setting up native I/O or invoking a callback. */
    @FunctionalInterface
    public interface IoOperation {
        void run() throws IOException;
    }

    /**
     * Notify an asynchronous caller of setup failure before allowing fatal errors to escape.
     * The handler may also rethrow a custom callback's exception to preserve its contract.
     * @param operation the I/O or callback operation
     * @param failureHandler cleanup and failure notification for its waiting caller
     */
    @SuppressWarnings("java:S1181") // This boundary must notify waiters even for callback Errors.
    public static void runWithFailureHandler(IoOperation operation, Consumer<Throwable> failureHandler) {
        try {
            operation.run();
        } catch (Throwable error) {
            try {
                failureHandler.accept(error);
            } finally {
                if (IoThreadClientConnection.isFatal(error)) IoThreadClientConnection.rethrow(error);
            }
        }
    }

    /** Adapt a provider callback at the connection boundary. */
    public static ClientCallback<ClientConnection> wrap(ClientCallback<ClientConnection> callback) {
        return new ClientCallback<>() {
            @Override public void completed(ClientConnection connection) { callback.completed(IoThreadClientConnection.wrap(connection)); }
            @Override public void failed(java.io.IOException error) { callback.failed(error); }
        };
    }

}
