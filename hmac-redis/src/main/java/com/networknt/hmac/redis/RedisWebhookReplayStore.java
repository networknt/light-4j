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
import com.networknt.hmac.ReplayStoreSummary;
import com.networknt.hmac.ReplayStoreUnavailableException;
import com.networknt.hmac.ReserveOutcome;
import com.networknt.hmac.WebhookReplayKey;
import com.networknt.hmac.WebhookReplayStore;
import com.networknt.server.ShutdownHookProvider;
import com.networknt.server.StartupHookProvider;
import org.redisson.Redisson;
import org.redisson.api.RFuture;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Redis replay provider using atomic Lua operations and Redisson's bounded timeouts. */
public final class RedisWebhookReplayStore
        implements WebhookReplayStore, StartupHookProvider, ShutdownHookProvider {
    public static final String DEFAULT_URL_ENV = "WEBHOOK_REPLAY_REDIS_URL";
    public static final String DEFAULT_KEY_PREFIX = "light:hmac-replay:";
    private static final Logger LOG = LoggerFactory.getLogger(RedisWebhookReplayStore.class);
    private static final String RESERVE_SCRIPT = "if redis.call('exists', KEYS[1]) == 0 then "
            + "redis.call('psetex', KEYS[1], ARGV[2], ARGV[1]); return 1 else return 0 end";
    private static final String RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) else return 0 end";
    private static final String REMOVE_SCRIPT = "return redis.call('del', KEYS[1])";

    private final Function<String, String> environment;
    private volatile RedisBackend backend;
    private String urlEnv = DEFAULT_URL_ENV;
    private String keyPrefix = DEFAULT_KEY_PREFIX;
    private int connectTimeoutMillis = 1000;
    private int operationTimeoutMillis = 1000;

    public RedisWebhookReplayStore() {
        this(System::getenv, null);
    }

    RedisWebhookReplayStore(Function<String, String> environment, RedisBackend backend) {
        this.environment = Objects.requireNonNull(environment);
        this.backend = backend;
    }

    public void setUrlEnv(String urlEnv) {
        this.urlEnv = urlEnv;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public void setOperationTimeoutMillis(int operationTimeoutMillis) {
        this.operationTimeoutMillis = operationTimeoutMillis;
    }

    @Override
    public CompletionStage<ReserveOutcome> reserve(WebhookReplayKey key, Duration retention) {
        if (key == null || retention == null || retention.isZero() || retention.isNegative())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Replay key and positive retention are required."));
        ReplayReservation reservation = ReplayReservation.create(key);
        long computedRetentionMillis;
        try {
            computedRetentionMillis = Math.max(1, retention.toMillis());
        } catch (ArithmeticException e) {
            computedRetentionMillis = Long.MAX_VALUE;
        }
        final long retentionMillis = computedRetentionMillis;
        return provider(() -> backend().reserve(storageKey(key), reservation.getOwnerToken(), retentionMillis))
                .thenApply(reserved -> reserved
                        ? new ReserveOutcome.Reserved(reservation)
                        : ReserveOutcome.Duplicate.INSTANCE);
    }

    @Override
    public CompletionStage<Void> release(ReplayReservation reservation) {
        if (reservation == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("reservation must not be null."));
        return provider(() -> backend().release(storageKey(reservation.getKey()), reservation.getOwnerToken()))
                .thenApply(ignored -> null);
    }

    @Override
    public CompletionStage<Boolean> forceRemove(WebhookReplayKey key) {
        if (key == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("key must not be null."));
        return provider(() -> backend().remove(storageKey(key)));
    }

    @Override
    public ReplayStoreScope getScope() {
        return ReplayStoreScope.DISTRIBUTED;
    }

    @Override
    public ReplayStoreSummary getSummary() {
        return new ReplayStoreSummary(getClass().getName(), getScope(), -1, -1);
    }

    @Override
    public void validate() {
        requireText(urlEnv, "urlEnv");
        requireText(keyPrefix, "keyPrefix");
        if (connectTimeoutMillis <= 0 || operationTimeoutMillis <= 0)
            throw new IllegalStateException("Redis replay timeouts must be positive.");
        if (backend == null) {
            String address = environment.apply(urlEnv);
            requireText(address, "Redis URL environment variable " + urlEnv);
            if (!address.startsWith("redis://") && !address.startsWith("rediss://"))
                throw new IllegalStateException("Redis replay URL must use redis:// or rediss://.");
            synchronized (this) {
                if (backend == null) {
                    try {
                        backend = new RedissonBackend(address, connectTimeoutMillis, operationTimeoutMillis);
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to initialize Redis HMAC replay provider.");
                    }
                }
            }
        }
    }

    @Override
    public void onStartup() {
        validate();
        LOG.info("HMAC replay store started with scope=distributed provider=redis.");
    }

    @Override
    public void onShutdown() {
        RedisBackend current = backend;
        if (current != null)
            current.close();
    }

    private RedisBackend backend() {
        validate();
        return backend;
    }

    private String storageKey(WebhookReplayKey key) {
        return keyPrefix + key.getDigest();
    }

    private <T> CompletionStage<T> provider(StageSupplier<T> supplier) {
        try {
            return supplier.get().handle((value, failure) -> {
                if (failure != null)
                    throw unavailable(failure);
                return value;
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(unavailable(e));
        }
    }

    private ReplayStoreUnavailableException unavailable(Throwable failure) {
        return new ReplayStoreUnavailableException(ReplayStoreUnavailableException.Reason.PROVIDER,
                "Redis HMAC replay store operation failed.");
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalStateException(field + " must not be blank.");
    }

    @FunctionalInterface
    private interface StageSupplier<T> {
        CompletionStage<T> get();
    }

    interface RedisBackend {
        CompletionStage<Boolean> reserve(String key, String owner, long retentionMillis);

        CompletionStage<Boolean> release(String key, String owner);

        CompletionStage<Boolean> remove(String key);

        void close();
    }

    private static final class RedissonBackend implements RedisBackend {
        private final RedissonClient client;
        private final RScript script;

        private RedissonBackend(String address, int connectTimeoutMillis, int operationTimeoutMillis) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress(address)
                    .setConnectTimeout(connectTimeoutMillis)
                    .setTimeout(operationTimeoutMillis)
                    .setRetryAttempts(0);
            client = Redisson.create(config);
            script = client.getScript(StringCodec.INSTANCE);
        }

        @Override
        public CompletionStage<Boolean> reserve(String key, String owner, long retentionMillis) {
            RFuture<Long> future = script.evalAsync(RScript.Mode.READ_WRITE, RESERVE_SCRIPT,
                    RScript.ReturnType.INTEGER, List.of(key), owner, retentionMillis);
            return future.thenApply(value -> value != null && value == 1L);
        }

        @Override
        public CompletionStage<Boolean> release(String key, String owner) {
            RFuture<Long> future = script.evalAsync(RScript.Mode.READ_WRITE, RELEASE_SCRIPT,
                    RScript.ReturnType.INTEGER, List.of(key), owner);
            return future.thenApply(value -> value != null && value == 1L);
        }

        @Override
        public CompletionStage<Boolean> remove(String key) {
            RFuture<Long> future = script.evalAsync(RScript.Mode.READ_WRITE, REMOVE_SCRIPT,
                    RScript.ReturnType.INTEGER, List.of(key));
            return future.thenApply(value -> value != null && value == 1L);
        }

        @Override
        public void close() {
            client.shutdown();
        }
    }
}
