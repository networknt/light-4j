package com.networknt.client.http;

import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Connection boundary for safe sends and logical close from any thread. Channel access,
 * upgrades and option changes retain Undertow's owning-I/O-thread contract.
 * A close retires this connection immediately; physical close runs on its owner thread.
 */
public final class IoThreadClientConnection implements ClientConnection {
    private static final Logger logger = LoggerFactory.getLogger(IoThreadClientConnection.class);
    private final ClientConnection delegate;
    private final AtomicBoolean closed = new AtomicBoolean();

    private IoThreadClientConnection(ClientConnection delegate) { this.delegate = delegate; }

    /** Return an idempotently guarded connection. */
    public static IoThreadClientConnection wrap(ClientConnection connection) {
        return connection instanceof IoThreadClientConnection owned ? owned : new IoThreadClientConnection(connection);
    }

    // Pool metadata must use the same key whether supplied a transport or its guarded view.
    static ClientConnection identity(ClientConnection connection) {
        return connection instanceof IoThreadClientConnection owned ? owned.delegate : connection;
    }

    @Override
    public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback) {
        sendRequest(request, callback, () -> true);
    }

    /** Evaluate readiness on the owner thread immediately before sending (for caller cancellation). */
    public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback, BooleanSupplier ready) {
        OnceCallback guarded = new OnceCallback(callback);
        Runnable send = () -> {
            try {
                if (!isOpen() || !ready.getAsBoolean() || !isOpen()) {
                    guarded.failed(new ClosedChannelException());
                    return;
                }
                delegate.sendRequest(request, guarded);
            } catch (Throwable error) {
                guarded.sendFailed(error);
            }
        };
        try {
            ClientConnectionOperations.execute(delegate, send);
        } catch (Throwable error) {
            guarded.sendFailed(error);
        }
    }

    @Override
    public boolean isOpen() { return !closed.get() && delegate.isOpen(); }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        Runnable close = () -> {
            try { delegate.close(); }
            catch (Throwable error) { logger.debug("Unable to close retired client connection", error); }
        };
        try { ClientConnectionOperations.execute(delegate, close); }
        catch (Throwable error) {
            // A stopped owner cannot safely execute native connection operations. The connection
            // remains retired, and worker shutdown owns cleanup of its underlying channels.
            logger.debug("Owner rejected close of retired client connection", error);
        }
    }

    private final class OnceCallback implements ClientCallback<ClientExchange> {
        private final ClientCallback<ClientExchange> callback;
        private final AtomicBoolean delivered = new AtomicBoolean();
        private OnceCallback(ClientCallback<ClientExchange> callback) { this.callback = callback; }

        @Override
        public void completed(ClientExchange exchange) {
            if (!delivered.compareAndSet(false, true)) return;
            try { callback.completed(new OwnedExchange(exchange)); }
            catch (Throwable error) { callbackFailed(error); }
        }

        @Override
        public void failed(IOException error) {
            if (!delivered.compareAndSet(false, true)) return;
            try { callback.failed(error); }
            catch (Throwable callbackError) { callbackFailed(callbackError); }
        }

        private void sendFailed(Throwable error) {
            if (!delivered.get()) failed(asIOException(error));
            else callbackFailed(error);
        }

        private void callbackFailed(Throwable error) {
            // Do not issue failed() after completed(). Future callbacks have a separate outcome
            // channel; framework latch callbacks handle their own setup failures internally.
            if (callback instanceof CompletableFuture<?> future) future.completeExceptionally(error);
            logger.error("Client callback threw after terminal notification", error);
            if (!isMultiplexingSupported()) close();
        }
    }

    static IOException asIOException(Throwable error) {
        return error instanceof IOException io ? io : new IOException("Client connection operation failed", error);
    }

    private final class OwnedExchange implements ClientExchange {
        private final ClientExchange exchange;
        private OwnedExchange(ClientExchange exchange) { this.exchange = exchange; }
        @Override public ClientConnection getConnection() { return IoThreadClientConnection.this; }
        @Override public void setResponseListener(ClientCallback<ClientExchange> listener) {
            exchange.setResponseListener(new ClientCallback<>() {
                @Override public void completed(ClientExchange response) { listener.completed(new OwnedExchange(response)); }
                @Override public void failed(IOException error) { listener.failed(error); }
            });
        }
        @Override public void setContinueHandler(ContinueNotification handler) {
            exchange.setContinueHandler(handler == null ? null : result -> handler.handleContinue(new OwnedExchange(result)));
        }
        @Override public void setPushHandler(PushCallback handler) {
            exchange.setPushHandler(handler == null ? null : (original, pushed) ->
                    handler.handlePush(new OwnedExchange(original), new OwnedExchange(pushed)));
        }
        @Override public StreamSinkChannel getRequestChannel() { return exchange.getRequestChannel(); }
        @Override public StreamSourceChannel getResponseChannel() { return exchange.getResponseChannel(); }
        @Override public ClientRequest getRequest() { return exchange.getRequest(); }
        @Override public ClientResponse getResponse() { return exchange.getResponse(); }
        @Override public ClientResponse getContinueResponse() { return exchange.getContinueResponse(); }
        @Override public <T> T getAttachment(AttachmentKey<T> key) { return exchange.getAttachment(key); }
        @Override public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key) { return exchange.getAttachmentList(key); }
        @Override public <T> T putAttachment(AttachmentKey<T> key, T value) { return exchange.putAttachment(key, value); }
        @Override public <T> T removeAttachment(AttachmentKey<T> key) { return exchange.removeAttachment(key); }
        @Override public <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> key, T value) { exchange.addToAttachmentList(key, value); }
    }

    @Override public StreamConnection performUpgrade() throws IOException { return delegate.performUpgrade(); }
    @Override public ByteBufferPool getBufferPool() { return delegate.getBufferPool(); }
    @Override public SocketAddress getPeerAddress() { return delegate.getPeerAddress(); }
    @Override public <A extends SocketAddress> A getPeerAddress(Class<A> type) { return delegate.getPeerAddress(type); }
    @Override public SocketAddress getLocalAddress() { return delegate.getLocalAddress(); }
    @Override public <A extends SocketAddress> A getLocalAddress(Class<A> type) { return delegate.getLocalAddress(type); }
    @Override public XnioWorker getWorker() { return delegate.getWorker(); }
    @Override public XnioIoThread getIoThread() { return delegate.getIoThread(); }
    @Override public boolean supportsOption(Option<?> option) { return delegate.supportsOption(option); }
    @Override public <T> T getOption(Option<T> option) throws IOException { return delegate.getOption(option); }
    @Override public <T> T setOption(Option<T> option, T value) throws IOException { return delegate.setOption(option, value); }
    @Override public boolean isUpgraded() { return delegate.isUpgraded(); }
    @Override public boolean isPushSupported() { return delegate.isPushSupported(); }
    @Override public boolean isMultiplexingSupported() { return delegate.isMultiplexingSupported(); }
    @Override public ClientStatistics getStatistics() { return delegate.getStatistics(); }
    @Override public boolean isUpgradeSupported() { return delegate.isUpgradeSupported(); }
    @Override public boolean isPingSupported() { return delegate.isPingSupported(); }
    @Override public void sendPing(PingListener listener, long timeout, TimeUnit unit) { delegate.sendPing(listener, timeout, unit); }
    @Override public ChannelListener.Setter<ClientConnection> getCloseSetter() {
        return listener -> ClientConnectionOperations.execute(delegate, () -> delegate.getCloseSetter()
                .set(listener == null ? null : ignored -> listener.handleEvent(this)));
    }
    @Override public void addCloseListener(ChannelListener<ClientConnection> listener) {
        ClientConnectionOperations.execute(delegate, () -> delegate.addCloseListener(ignored -> listener.handleEvent(this)));
    }
}
