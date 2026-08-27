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

package com.networknt.hmac.redis;

import com.networknt.hmac.ReplayReservation;
import com.networknt.hmac.ReplayStoreScope;
import com.networknt.hmac.ReplayStoreUnavailableException;
import com.networknt.hmac.ReserveOutcome;
import com.networknt.hmac.WebhookReplayKey;
import com.networknt.hmac.WebhookReplayStore;
import com.networknt.server.ShutdownHookProvider;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RedisWebhookReplayStoreTest {
    @Test
    void adapterUsesHashedKeysAndOwnerCheckedAtomicOperations() {
        InMemoryBackend backend = new InMemoryBackend();
        RedisWebhookReplayStore store = new RedisWebhookReplayStore(name -> null, backend);
        WebhookReplayKey key = new WebhookReplayKey("github", "selector-123", "delivery-456");

        ReplayReservation first = ((ReserveOutcome.Reserved) store.reserve(key, Duration.ofMinutes(1))
                .toCompletableFuture().join()).reservation();
        Assertions.assertSame(ReserveOutcome.Duplicate.INSTANCE,
                store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
        Assertions.assertFalse(backend.lastKey.contains("selector-123"));
        Assertions.assertFalse(backend.lastKey.contains("delivery-456"));

        backend.values.remove(backend.lastKey);
        ReplayReservation newer = ((ReserveOutcome.Reserved) store.reserve(key, Duration.ofMinutes(1))
                .toCompletableFuture().join()).reservation();
        store.release(first).toCompletableFuture().join();
        Assertions.assertSame(ReserveOutcome.Duplicate.INSTANCE,
                store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
        store.release(newer).toCompletableFuture().join();
        Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
        Assertions.assertTrue(store.forceRemove(key).toCompletableFuture().join());
    }

    @Test
    void providerOutageFailsClosedWithoutLocalFallback() {
        RedisWebhookReplayStore store = new RedisWebhookReplayStore(name -> null, new FailingBackend());
        CompletionException failure = Assertions.assertThrows(CompletionException.class,
                () -> store.reserve(new WebhookReplayKey("github", "shared", "delivery"), Duration.ofMinutes(1))
                        .toCompletableFuture().join());
        ReplayStoreUnavailableException unavailable =
                Assertions.assertInstanceOf(ReplayStoreUnavailableException.class, failure.getCause());
        Assertions.assertEquals(ReplayStoreUnavailableException.Reason.PROVIDER, unavailable.getReason());
        Assertions.assertEquals(ReplayStoreScope.DISTRIBUTED, store.getScope());
        Assertions.assertEquals(-1, store.getSummary().entries());
    }

    @Test
    void validatesConfigurationAndServiceBindingSharesLifecycleObject() {
        RedisWebhookReplayStore missing = new RedisWebhookReplayStore(name -> null, null);
        Assertions.assertThrows(IllegalStateException.class, missing::validate);
        RedisWebhookReplayStore invalidScheme = new RedisWebhookReplayStore(name -> "http://localhost:6379", null);
        Assertions.assertThrows(IllegalStateException.class, invalidScheme::validate);

        WebhookReplayStore[] stores = SingletonServiceFactory.getBeans(WebhookReplayStore.class);
        StartupHookProvider[] startup = SingletonServiceFactory.getBeans(StartupHookProvider.class);
        ShutdownHookProvider[] shutdown = SingletonServiceFactory.getBeans(ShutdownHookProvider.class);
        Assertions.assertNotNull(stores);
        Assertions.assertEquals(1, stores.length);
        Assertions.assertInstanceOf(RedisWebhookReplayStore.class, stores[0]);
        Assertions.assertSame(stores[0], startup[0]);
        Assertions.assertSame(stores[0], shutdown[0]);
    }

    @Test
    void concurrentAdaptersShareOneDistributedReservationAcrossRestarts() {
        InMemoryBackend redis = new InMemoryBackend();
        RedisWebhookReplayStore firstProcess = new RedisWebhookReplayStore(name -> null, redis);
        RedisWebhookReplayStore secondProcess = new RedisWebhookReplayStore(name -> null, redis);
        WebhookReplayKey key = new WebhookReplayKey("github", "shared", "distributed-race");
        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<CompletableFuture<ReserveOutcome>> attempts = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                RedisWebhookReplayStore process = i % 2 == 0 ? firstProcess : secondProcess;
                attempts.add(CompletableFuture.supplyAsync(() -> process.reserve(key, Duration.ofMinutes(1))
                        .toCompletableFuture().join(), executor));
            }
            CompletableFuture.allOf(attempts.toArray(CompletableFuture[]::new)).join();
            Assertions.assertEquals(1, attempts.stream().map(CompletableFuture::join)
                    .filter(ReserveOutcome.Reserved.class::isInstance).count());
            RedisWebhookReplayStore restartedProcess = new RedisWebhookReplayStore(name -> null, redis);
            Assertions.assertSame(ReserveOutcome.Duplicate.INSTANCE,
                    restartedProcess.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class InMemoryBackend implements RedisWebhookReplayStore.RedisBackend {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        private volatile String lastKey;

        @Override
        public CompletionStage<Boolean> reserve(String key, String owner, long retentionMillis) {
            lastKey = key;
            return CompletableFuture.completedFuture(values.putIfAbsent(key, owner) == null);
        }

        @Override
        public CompletionStage<Boolean> release(String key, String owner) {
            lastKey = key;
            return CompletableFuture.completedFuture(values.remove(key, owner));
        }

        @Override
        public CompletionStage<Boolean> remove(String key) {
            lastKey = key;
            return CompletableFuture.completedFuture(values.remove(key) != null);
        }

        @Override public void close() { }
    }

    private static final class FailingBackend implements RedisWebhookReplayStore.RedisBackend {
        private <T> CompletionStage<T> failed() {
            return CompletableFuture.failedFuture(new IllegalStateException("redis unavailable"));
        }
        @Override public CompletionStage<Boolean> reserve(String key, String owner, long retentionMillis) { return failed(); }
        @Override public CompletionStage<Boolean> release(String key, String owner) { return failed(); }
        @Override public CompletionStage<Boolean> remove(String key) { return failed(); }
        @Override public void close() { }
    }
}
