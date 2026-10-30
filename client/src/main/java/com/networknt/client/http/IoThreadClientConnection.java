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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canonical connection view: sendRequest/sendPing dispatch to the owner; close retires immediately.
 * Synchronous configuration, upgrade and exchange access retain the delegate's contract.
 * This is not a general thread-safe channel adapter. Custom callbacks must signal their own
 * waiters in finally: a throwing callback is never followed by a second terminal notification.
 * Close-listener registration is synchronous. Late listeners (or notifications after owner
 * shutdown) may run on the registering/closing thread, without accessing native channels.
 */
public final class IoThreadClientConnection implements ClientConnection {
    private static final Logger logger = LoggerFactory.getLogger(IoThreadClientConnection.class);
    private final ClientConnection delegate;
    private final AtomicBoolean closed;
    private final AtomicBoolean closeBridgeInstalled = new AtomicBoolean();
    private final ConnectionCloseListeners closeListeners = new ConnectionCloseListeners(this);
    private final ChannelListener.Setter<ClientConnection> closeSetter = listener -> {
        closeListeners.set(listener);
        if (listener != null) installCloseBridge();
    };
    private final ConnectionWrappers.Entry state;

    IoThreadClientConnection(ClientConnection delegate, ConnectionWrappers.Entry state) {
        this.delegate = delegate;
        this.closed = state.closed;
        this.state = state;
        if (this.closed.get()) closeListeners.fire();
    }
    /** Return the same live view for the same raw transport, without retaining dead transports. */
    public static IoThreadClientConnection wrap(ClientConnection connection) {
        Objects.requireNonNull(connection, "connection");
        IoThreadClientConnection owned = connection instanceof IoThreadClientConnection view ? view : ConnectionWrappers.wrap(connection);
        // Native registration (and any close notification) runs outside the registry monitor.
        // The bridge retains the complete view while the transport is alive.
        owned.installCloseBridge();
        return owned;
    }
    static ClientConnection identity(ClientConnection connection) {
        return connection instanceof IoThreadClientConnection owned ? owned.delegate : connection;
    }
    @Override public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback) {
        sendRequest(request, callback, new RequestSubmission());
    }
    /** Admission is explicit and occurs once, after the open check and before native handoff. */
    public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback, RequestSubmission submission) {
        OnceCallback guarded = new OnceCallback(callback, submission);
        Runnable send = () -> ClientConnectionOperations.runWithFailureHandler(() -> {
            if (!isOpen()) { guarded.failed(state.closeReason == null ? new ClosedChannelException() : state.closeReason); return; }
            if (!submission.start()) { guarded.notSent(new IOException("Request submission cancelled")); return; }
            delegate.sendRequest(request, guarded);
        }, guarded::sendFailed);
        if (Thread.currentThread() == getIoThread()) send.run();
        else ClientConnectionOperations.runWithFailureHandler(() -> getIoThread().execute(send), guarded::sendFailed);
    }
    @Override public boolean isOpen() { return !closed.get() && delegate.isOpen(); }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        Runnable close = () -> {
            try { delegate.close(); }
            catch (IOException | RuntimeException error) { logger.debug("Unable to close retired client connection", error); }
            finally { closeListeners.fire(); }
        };
        if (Thread.currentThread() == getIoThread()) close.run();
        else {
            try { getIoThread().execute(close); }
            catch (java.util.concurrent.RejectedExecutionException error) {
                state.closeReason = asIOException(error);
                closeListeners.fire();
            }
        }
    }
    private void installCloseBridge() {
        if (closed.get() || !closeBridgeInstalled.compareAndSet(false, true)) return;
        Runnable install = () -> {
            delegate.addCloseListener(ignored -> { closed.set(true); closeListeners.fire(); });
            if (!delegate.isOpen()) { closed.set(true); closeListeners.fire(); }
        };
        try { ClientConnectionOperations.execute(delegate, install); }
        catch (java.util.concurrent.RejectedExecutionException error) {
            closed.set(true);
            state.closeReason = asIOException(error);
            closeListeners.fire();
        }
    }
    @Override public ChannelListener.Setter<ClientConnection> getCloseSetter() { return closeSetter; }
    @Override public void addCloseListener(ChannelListener<ClientConnection> listener) {
        closeListeners.add(listener);
        installCloseBridge();
    }

    private final class OnceCallback implements ClientCallback<ClientExchange> {
        private final ClientCallback<ClientExchange> callback;
        private final AtomicBoolean delivered = new AtomicBoolean();
        private volatile Throwable callbackError;
        private OwnedExchange exchange;
        private final RequestSubmission submission;
        OnceCallback(ClientCallback<ClientExchange> callback, RequestSubmission submission) {
            this.callback = callback;
            this.submission = submission;
        }
        @Override public void completed(ClientExchange raw) {
            if (!delivered.compareAndSet(false, true)) return;
            exchange = new OwnedExchange(raw, new ExchangeState());
            submission.onCancellation(error -> exchange.abort(error, () -> { /* The caller future already carries cancellation. */ }));
            ClientConnectionOperations.runWithFailureHandler(() -> {
                if (exchange.state.aborted.get()) callback.failed(new IOException("Request cancelled"));
                else callback.completed(exchange);
            }, error -> {
                callbackError = error;
                callbackFailed(error);
            });
        }
        @Override public void failed(IOException error) {
            if (!delivered.compareAndSet(false, true)) return;
            if (!isMultiplexingSupported()) close();
            ClientConnectionOperations.runWithFailureHandler(() -> callback.failed(error), failure -> {
                callbackError = failure;
                callbackFailed(failure);
            });
        }
        void notSent(IOException error) {
            if (!delivered.compareAndSet(false, true)) return;
            ClientConnectionOperations.runWithFailureHandler(() -> callback.failed(error), failure -> {
                callbackError = failure;
                if (callback instanceof CompletableFuture<?> future) future.completeExceptionally(failure);
                else rethrow(failure);
            });
        }
        void sendFailed(Throwable error) {
            if (error == callbackError) { rethrow(error); return; }
            if (!delivered.get()) failed(asIOException(error));
            else callbackFailed(error);
            if (isFatal(error)) rethrow(error);
        }
        private void callbackFailed(Throwable error) {
            Runnable complete = () -> {
                if (callback instanceof CompletableFuture<?> future) future.completeExceptionally(error);
            };
            if (exchange != null) exchange.abort(error, complete);
            else {
                if (!isMultiplexingSupported()) close();
                complete.run();
            }
            if (!(callback instanceof CompletableFuture<?>) || isFatal(error)) rethrow(error);
        }
    }
    static IOException asIOException(Throwable error) {
        return error instanceof IOException io ? io : new IOException("Client connection operation failed", error);
    }
    static boolean isFatal(Throwable error) {
        return error instanceof VirtualMachineError || error instanceof LinkageError;
    }
    static void rethrow(Throwable error) {
        if (error instanceof Error fatal) throw fatal;
        if (error instanceof RuntimeException runtime) throw runtime;
        throw new IllegalStateException(error);
    }
    static ClientExchange ownExchange(ClientExchange exchange) {
        if (exchange instanceof IoThreadClientConnection.OwnedExchange) return exchange;
        IoThreadClientConnection owner = wrap(exchange.getConnection());
        return owner.new OwnedExchange(exchange, new ExchangeState());
    }
    static void abortExchange(ClientExchange exchange, Throwable error, Runnable afterRetirement) {
        ((OwnedExchange) ownExchange(exchange)).abort(error, afterRetirement);
    }
    private static final class ExchangeState {
        final AtomicBoolean aborted = new AtomicBoolean();
        final CompletableFuture<Void> retired = new CompletableFuture<>();
    }
    private final class OwnedExchange implements ClientExchange {
        private final ClientExchange exchange;
        private final ExchangeState state;
        OwnedExchange(ClientExchange exchange, ExchangeState state) { this.exchange = exchange; this.state = state; }
        void abort(Throwable original, Runnable afterRetirement) {
            if (!state.aborted.compareAndSet(false, true)) {
                state.retired.thenRun(afterRetirement);
                return;
            }
            Runnable cleanup = () -> {
                try {
                    if (!isMultiplexingSupported()) close();
                    else {
                        cleanup(() -> closeRequestChannel(exchange.getRequestChannel()), original);
                        cleanup(() -> closeResponseChannel(exchange.getResponseChannel()), original);
                    }
                } finally { state.retired.complete(null); afterRetirement.run(); }
            };
            if (Thread.currentThread() == getIoThread()) cleanup.run();
            else {
                try { getIoThread().execute(cleanup); }
                catch (java.util.concurrent.RejectedExecutionException _) {
                    // The owner cannot execute more stream callbacks. Quarantine remains in force.
                    if (!isMultiplexingSupported()) close();
                    state.retired.complete(null);
                    afterRetirement.run();
                }
            }
        }
        private void closeRequestChannel(StreamSinkChannel channel) {
            if (channel == null) return;
            try {
                channel.suspendWrites();
                if (channel.getWriteSetter() != null) channel.getWriteSetter().set(null);
            } finally { IoUtils.safeClose(channel); }
        }
        private void closeResponseChannel(StreamSourceChannel channel) {
            if (channel == null) return;
            try {
                channel.suspendReads();
                if (channel.getReadSetter() != null) channel.getReadSetter().set(null);
            } finally { IoUtils.safeClose(channel); }
        }
        private void cleanup(Runnable action, Throwable original) {
            ClientConnectionOperations.runWithFailureHandler(action::run, error -> {
                if (error != original) original.addSuppressed(error);
            });
        }
        @Override public ClientConnection getConnection() { return IoThreadClientConnection.this; }
        @Override public void setResponseListener(ClientCallback<ClientExchange> listener) {
            exchange.setResponseListener(new ClientCallback<>() {
                @Override public void completed(ClientExchange response) {
                    if (!state.aborted.get()) listener.completed(new OwnedExchange(response, state));
                }
                @Override public void failed(IOException error) { if (!state.aborted.get()) listener.failed(error); }
            });
        }
        @Override public void setContinueHandler(ContinueNotification handler) {
            exchange.setContinueHandler(handler == null ? null : result -> {
                if (!state.aborted.get()) handler.handleContinue(new OwnedExchange(result, state));
            });
        }
        @Override public void setPushHandler(PushCallback handler) {
            exchange.setPushHandler(handler == null ? null : (original, pushed) -> !state.aborted.get() &&
                    handler.handlePush(new OwnedExchange(original, state), new OwnedExchange(pushed, new ExchangeState())));
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

    @Override public void sendPing(PingListener listener, long timeout, TimeUnit unit) {
        OncePingListener once = new OncePingListener(listener);
        Runnable ping = () -> ClientConnectionOperations.runWithFailureHandler(() -> {
            if (!isOpen()) once.failed(new ClosedChannelException());
            else delegate.sendPing(once, timeout, unit);
        }, once::sendFailed);
        if (Thread.currentThread() == getIoThread()) ping.run();
        else ClientConnectionOperations.runWithFailureHandler(() -> getIoThread().execute(ping), once::sendFailed);
    }

    private static final class OncePingListener implements PingListener {
        private final PingListener listener;
        private final AtomicBoolean notified = new AtomicBoolean();
        OncePingListener(PingListener listener) { this.listener = listener; }
        @Override public void acknowledged() {
            if (notified.compareAndSet(false, true)) listener.acknowledged();
        }
        @Override public void failed(IOException error) {
            if (notified.compareAndSet(false, true)) listener.failed(error);
        }
        void sendFailed(Throwable error) {
            if (notified.get()) rethrow(error);
            failed(asIOException(error));
        }
    }
}
