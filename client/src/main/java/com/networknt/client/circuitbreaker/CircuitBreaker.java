/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.circuitbreaker;

import com.networknt.client.ClientConfig;
import io.undertow.client.ClientResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation based on the configuration in client.yml
 *
 * @author Jeferson Perito
 */
public class CircuitBreaker {

    private Supplier<CompletableFuture<ClientResponse>> supplier;
    private static AtomicInteger timeoutCount;
    private long lastErrorTime;

    public CircuitBreaker(Supplier<CompletableFuture<ClientResponse>> supplier) {
        this.supplier = supplier;
        this.timeoutCount = new AtomicInteger(0);
    }

    public ClientResponse call() throws TimeoutException, ExecutionException, InterruptedException {
        State state = checkState();

        try {
            if (State.OPEN == state) {
                throw new IllegalStateException("circuit is opened.");
            }

            ClientResponse clientResponse = supplier.get().get(ClientConfig.get().getTimeout(), TimeUnit.MILLISECONDS);
            timeoutCount = new AtomicInteger(0);

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
        boolean isExtrapolatedErrorThreshold = timeoutCount.get() >= clientConfig.getErrorThreshold();
        if (isExtrapolatedErrorThreshold && isExtrapolatedResetTimeout) {
            return State.HALF_OPEN;
        }
        if (timeoutCount.get() >= clientConfig.getErrorThreshold()) {
            return State.OPEN;
        }
        return State.CLOSE;
    }

    private void recordTimeout() {
        timeoutCount.getAndIncrement();
        lastErrorTime = Instant.now().toEpochMilli();
    }
}
