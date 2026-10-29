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
 * Synchronous configuration, upgrade and exchange-channel access require the owning I/O thread.
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
    private volatile IOException closeReason;

    IoThreadClientConnection(ClientConnection delegate, AtomicBoolean closed) {
        this.delegate = delegate;
        this.closed = closed;
        if (closed.get()) closeListeners.fire();
    }
    /** Return the same live view for the same raw transport, without retaining dead transports. */
    public static IoThreadClientConnection wrap(ClientConnection connection) {
        Objects.requireNonNull(connection, "connection");
        return connection instanceof IoThreadClientConnection owned ? owned : ConnectionWrappers.wrap(connection);
    }
    static ClientConnection identity(ClientConnection connection) {
        return connection instanceof IoThreadClientConnection owned ? owned.delegate : connection;
    }
    private void requireOwner() {
        if (Thread.currentThread() != getIoThread()) throw new IllegalStateException("Operation requires the connection's owning I/O thread");
    }
    @Override public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback) {
        sendRequest(request, callback, new RequestSubmission());
    }
    /** Admission is explicit and occurs once, after the open check and before native handoff. */
    public void sendRequest(ClientRequest request, ClientCallback<ClientExchange> callback, RequestSubmission submission) {
        OnceCallback guarded = new OnceCallback(callback);
        Runnable send = () -> {
            try {
                if (!isOpen()) { guarded.failed(closeReason == null ? new ClosedChannelException() : closeReason); return; }
                if (!submission.start()) { guarded.failed(new IOException("Request submission cancelled")); return; }
                delegate.sendRequest(request, guarded);
            } catch (Throwable error) { guarded.sendFailed(error); }
        };
        if (Thread.currentThread() == getIoThread()) send.run();
        else {
            try { getIoThread().execute(send); }
            catch (Throwable error) { guarded.sendFailed(error); }
        }
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
                closeReason = asIOException(error);
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
            closeReason = asIOException(error);
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
        OnceCallback(ClientCallback<ClientExchange> callback) { this.callback = callback; }
        @Override public void completed(ClientExchange raw) {
            if (!delivered.compareAndSet(false, true)) return;
            exchange = new OwnedExchange(raw, new ExchangeState());
            try { callback.completed(exchange); }
            catch (Throwable error) { callbackError = error; callbackFailed(error); }
        }
        @Override public void failed(IOException error) {
            if (!delivered.compareAndSet(false, true)) return;
            if (!isMultiplexingSupported()) close();
            try { callback.failed(error); }
            catch (Throwable failure) { callbackError = failure; callbackFailed(failure); }
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
            else { if (!isMultiplexingSupported()) close(); complete.run(); }
            if (!(callback instanceof CompletableFuture<?>) || isFatal(error)) rethrow(error);
        }
    }
    static IOException asIOException(Throwable error) {
        return error instanceof IOException io ? io : new IOException("Client connection operation failed", error);
    }
    static boolean isFatal(Throwable error) {
        return error instanceof VirtualMachineError || error instanceof ThreadDeath || error instanceof LinkageError;
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
        private void requireActive() {
            if (state.aborted.get()) throw new IllegalStateException("Client exchange has been retired");
        }
        void abort(Throwable original, Runnable afterRetirement) {
            if (!state.aborted.compareAndSet(false, true)) {
                state.retired.thenRun(afterRetirement);
                return;
            }
            Runnable cleanup = () -> {
                try {
                    if (!isMultiplexingSupported()) close();
                    else {
                        cleanup(() -> IoUtils.safeClose(exchange.getRequestChannel()), original);
                        cleanup(() -> IoUtils.safeClose(exchange.getResponseChannel()), original);
                    }
                } finally { state.retired.complete(null); afterRetirement.run(); }
            };
            if (Thread.currentThread() == getIoThread()) cleanup.run();
            else {
                try { getIoThread().execute(cleanup); }
                catch (java.util.concurrent.RejectedExecutionException rejected) {
                    // The owner cannot execute more stream callbacks. Quarantine remains in force.
                    if (!isMultiplexingSupported()) close();
                    state.retired.complete(null);
                    afterRetirement.run();
                }
            }
        }
        private void cleanup(Runnable action, Throwable original) {
            try { action.run(); }
            catch (Throwable error) {
                if (error != original) original.addSuppressed(error);
                if (isFatal(error)) rethrow(error);
            }
        }
        @Override public ClientConnection getConnection() { return IoThreadClientConnection.this; }
        @Override public void setResponseListener(ClientCallback<ClientExchange> listener) {
            requireOwner(); requireActive();
            exchange.setResponseListener(new ClientCallback<>() {
                @Override public void completed(ClientExchange response) {
                    if (!state.aborted.get()) listener.completed(new OwnedExchange(response, state));
                }
                @Override public void failed(IOException error) { if (!state.aborted.get()) listener.failed(error); }
            });
        }
        @Override public void setContinueHandler(ContinueNotification handler) {
            requireOwner(); requireActive();
            exchange.setContinueHandler(handler == null ? null : result -> {
                if (!state.aborted.get()) handler.handleContinue(new OwnedExchange(result, state));
            });
        }
        @Override public void setPushHandler(PushCallback handler) {
            requireOwner(); requireActive();
            exchange.setPushHandler(handler == null ? null : (original, pushed) -> !state.aborted.get() &&
                    handler.handlePush(new OwnedExchange(original, state), new OwnedExchange(pushed, new ExchangeState())));
        }
        @Override public StreamSinkChannel getRequestChannel() { requireOwner(); requireActive(); return exchange.getRequestChannel(); }
        @Override public StreamSourceChannel getResponseChannel() { requireOwner(); requireActive(); return exchange.getResponseChannel(); }
        @Override public ClientRequest getRequest() { return exchange.getRequest(); }
        @Override public ClientResponse getResponse() { requireActive(); return exchange.getResponse(); }
        @Override public ClientResponse getContinueResponse() { requireActive(); return exchange.getContinueResponse(); }
        @Override public <T> T getAttachment(AttachmentKey<T> key) { return exchange.getAttachment(key); }
        @Override public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key) { return exchange.getAttachmentList(key); }
        @Override public <T> T putAttachment(AttachmentKey<T> key, T value) { requireActive(); return exchange.putAttachment(key, value); }
        @Override public <T> T removeAttachment(AttachmentKey<T> key) { return exchange.removeAttachment(key); }
        @Override public <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> key, T value) { requireActive(); exchange.addToAttachmentList(key, value); }
    }
    @Override public StreamConnection performUpgrade() throws IOException { requireOwner(); return delegate.performUpgrade(); }
    @Override public ByteBufferPool getBufferPool() { return delegate.getBufferPool(); }
    @Override public SocketAddress getPeerAddress() { return delegate.getPeerAddress(); }
    @Override public <A extends SocketAddress> A getPeerAddress(Class<A> type) { return delegate.getPeerAddress(type); }
    @Override public SocketAddress getLocalAddress() { return delegate.getLocalAddress(); }
    @Override public <A extends SocketAddress> A getLocalAddress(Class<A> type) { return delegate.getLocalAddress(type); }
    @Override public XnioWorker getWorker() { return delegate.getWorker(); }
    @Override public XnioIoThread getIoThread() { return delegate.getIoThread(); }
    @Override public boolean supportsOption(Option<?> option) { requireOwner(); return delegate.supportsOption(option); }
    @Override public <T> T getOption(Option<T> option) throws IOException { requireOwner(); return delegate.getOption(option); }
    @Override public <T> T setOption(Option<T> option, T value) throws IOException { requireOwner(); return delegate.setOption(option, value); }
    @Override public boolean isUpgraded() { return delegate.isUpgraded(); }
    @Override public boolean isPushSupported() { return delegate.isPushSupported(); }
    @Override public boolean isMultiplexingSupported() { return delegate.isMultiplexingSupported(); }
    @Override public ClientStatistics getStatistics() { return delegate.getStatistics(); }
    @Override public boolean isUpgradeSupported() { return delegate.isUpgradeSupported(); }
    @Override public boolean isPingSupported() { return delegate.isPingSupported(); }

    @Override public void sendPing(PingListener listener, long timeout, TimeUnit unit) {
        AtomicBoolean notified = new AtomicBoolean();
        PingListener once = new PingListener() {
            @Override public void acknowledged() { if (notified.compareAndSet(false, true)) listener.acknowledged(); }
            @Override public void failed(IOException error) { if (notified.compareAndSet(false, true)) listener.failed(error); }
        };
        Runnable ping = () -> {
            try {
                if (!isOpen()) once.failed(new ClosedChannelException());
                else delegate.sendPing(once, timeout, unit);
            } catch (Throwable error) {
                if (notified.get()) rethrow(error);
                once.failed(asIOException(error));
                if (isFatal(error)) rethrow(error);
            }
        };
        if (Thread.currentThread() == getIoThread()) ping.run();
        else {
            try { getIoThread().execute(ping); }
            catch (Throwable error) {
                if (notified.get()) rethrow(error);
                once.failed(asIOException(error));
                if (isFatal(error)) rethrow(error);
            }
        }
    }
}
