package com.networknt.client.circuitbreaker;

import com.networknt.client.ClientConfig;
import io.undertow.client.ClientResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class CircuitBreaker {

    private Supplier<CompletableFuture<ClientResponse>> supplier;
    private int timeoutCount;
    private long lastErrorTime;

    public CircuitBreaker(Supplier<CompletableFuture<ClientResponse>> supplier) {
        this.supplier = supplier;
        this.timeoutCount = 0;
    }

    public ClientResponse call() throws TimeoutException, ExecutionException, InterruptedException {
        State state = checkState();

        try {
            if (State.OPEN == state) {
                throw new IllegalStateException("circuit is opened.");
            }

            ClientResponse clientResponse = supplier.get().get(ClientConfig.get().getTimeout(), TimeUnit.MILLISECONDS);
            timeoutCount = 0;

            return clientResponse;
        } catch (InterruptedException | ExecutionException e) {
            throw e;
        } catch (TimeoutException e) {
            recordTimeout();
            throw e;
        }
    }

    private State checkState() {
        ClientConfig clientConfig = ClientConfig.get();

        boolean isExtrapolatedResetTimeout = Instant.now().toEpochMilli() - lastErrorTime > clientConfig.getResetTimeout();
        boolean isExtrapolatedErrorThreshold = timeoutCount >= clientConfig.getErrorThreshold();
        if (isExtrapolatedErrorThreshold && isExtrapolatedResetTimeout) {
            return State.HALF_OPEN;
        }
        if (timeoutCount >= clientConfig.getErrorThreshold()) {
            return State.OPEN;
        }
        return State.CLOSE;
    }

    private void recordTimeout() {
        timeoutCount++;
        lastErrorTime = Instant.now().toEpochMilli();
    }
}
