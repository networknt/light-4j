package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import java.io.IOException;

/** Framework callbacks use an internal failure path if their own channel setup throws. */
public abstract class SafeClientCallback<T> implements ClientCallback<T> {
    @Override
    public final void completed(T result) {
        try { completedSafely(result); }
        catch (Throwable error) { failedSafely(IoThreadClientConnection.asIOException(error)); }
    }
    @Override
    public final void failed(IOException error) { failedSafely(error); }
    protected abstract void completedSafely(T result);
    protected abstract void failedSafely(IOException error);
}
