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

package com.networknt.hmac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/** Bounded process-local replay store that never evicts an unexpired reservation. */
public final class LocalWebhookReplayStore implements WebhookReplayStore {
    public static final int DEFAULT_MAX_ENTRIES = 100_000;
    private static final Logger LOG = LoggerFactory.getLogger(LocalWebhookReplayStore.class);

    private final ConcurrentHashMap<WebhookReplayKey, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final LongSupplier nanoTime;
    private volatile int maxEntries = DEFAULT_MAX_ENTRIES;

    public LocalWebhookReplayStore() {
        this(System::nanoTime);
    }

    LocalWebhookReplayStore(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        LOG.warn("HMAC replay store started with scope=local; state is process-local and is lost on restart.");
    }

    public void setMaxEntries(int maxEntries) {
        if (maxEntries <= 0)
            throw new IllegalArgumentException("maxEntries must be positive.");
        if (!entries.isEmpty())
            throw new IllegalStateException("maxEntries cannot change after the replay store is used.");
        this.maxEntries = maxEntries;
    }

    @Override
    public CompletionStage<ReserveOutcome> reserve(WebhookReplayKey key, Duration retention) {
        if (key == null || retention == null || retention.isZero() || retention.isNegative())
            return failed(new IllegalArgumentException("Replay key and positive retention are required."));
        long expiresAt = expiresAt(retention);
        for (;;) {
            long now = nanoTime.getAsLong();
            Entry current = entries.get(key);
            if (current != null) {
                if (current.expiresAtNanos > now)
                    return CompletableFuture.completedFuture(ReserveOutcome.Duplicate.INSTANCE);
                remove(key, current);
                continue;
            }
            if (!acquireCapacity()) {
                removeExpired(now);
                if (!acquireCapacity())
                    return failed(new ReplayStoreUnavailableException(
                            ReplayStoreUnavailableException.Reason.CAPACITY,
                            "Local HMAC replay store capacity is exhausted."));
            }
            ReplayReservation reservation = ReplayReservation.create(key);
            Entry entry = new Entry(reservation.getOwnerToken(), expiresAt);
            Entry raced = entries.putIfAbsent(key, entry);
            if (raced == null)
                return CompletableFuture.completedFuture(new ReserveOutcome.Reserved(reservation));
            size.decrementAndGet();
            if (raced.expiresAtNanos <= nanoTime.getAsLong()) {
                remove(key, raced);
                continue;
            }
            return CompletableFuture.completedFuture(ReserveOutcome.Duplicate.INSTANCE);
        }
    }

    @Override
    public CompletionStage<Void> release(ReplayReservation reservation) {
        if (reservation == null)
            return failed(new IllegalArgumentException("reservation must not be null."));
        WebhookReplayKey key = reservation.getKey();
        Entry current = entries.get(key);
        if (current != null && current.ownerToken.equals(reservation.getOwnerToken()))
            remove(key, current);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Boolean> forceRemove(WebhookReplayKey key) {
        if (key == null)
            return failed(new IllegalArgumentException("key must not be null."));
        Entry removed = entries.remove(key);
        if (removed != null)
            size.decrementAndGet();
        return CompletableFuture.completedFuture(removed != null);
    }

    @Override
    public ReplayStoreScope getScope() {
        return ReplayStoreScope.LOCAL;
    }

    @Override
    public ReplayStoreSummary getSummary() {
        removeExpired(nanoTime.getAsLong());
        return new ReplayStoreSummary(getClass().getName(), getScope(), size.get(), maxEntries);
    }

    @Override
    public void validate() {
        if (maxEntries <= 0)
            throw new IllegalStateException("maxEntries must be positive.");
    }

    private boolean acquireCapacity() {
        for (;;) {
            int current = size.get();
            if (current >= maxEntries)
                return false;
            if (size.compareAndSet(current, current + 1))
                return true;
        }
    }

    private void removeExpired(long now) {
        for (Map.Entry<WebhookReplayKey, Entry> entry : entries.entrySet()) {
            if (entry.getValue().expiresAtNanos <= now)
                remove(entry.getKey(), entry.getValue());
        }
    }

    private void remove(WebhookReplayKey key, Entry entry) {
        if (entries.remove(key, entry))
            size.decrementAndGet();
    }

    private long expiresAt(Duration retention) {
        long nanos;
        try {
            nanos = retention.toNanos();
        } catch (ArithmeticException e) {
            nanos = Long.MAX_VALUE;
        }
        long now = nanoTime.getAsLong();
        try {
            return Math.addExact(now, nanos);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static <T> CompletionStage<T> failed(Throwable throwable) {
        return CompletableFuture.failedFuture(throwable);
    }

    private record Entry(String ownerToken, long expiresAtNanos) {
    }
}
