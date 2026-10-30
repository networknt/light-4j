package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientExchange;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Framework exchange callbacks quarantine failed setup before publishing failure. */
public abstract class SafeClientCallback implements ClientCallback<ClientExchange> {
    private final AtomicBoolean notified = new AtomicBoolean();
    @Override public final void completed(ClientExchange result) {
        if (!notified.compareAndSet(false, true)) return;
        ClientExchange owned = IoThreadClientConnection.ownExchange(result);
        ClientConnectionOperations.runWithFailureHandler(() -> completedSafely(owned), error ->
                IoThreadClientConnection.abortExchange(owned, error,
                        () -> failedSafely(IoThreadClientConnection.asIOException(error))));
    }
    @Override public final void failed(IOException error) {
        if (notified.compareAndSet(false, true)) failedSafely(error);
    }
    protected abstract void completedSafely(ClientExchange result);
    protected abstract void failedSafely(IOException error);
}
