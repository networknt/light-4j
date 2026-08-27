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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

class LocalWebhookReplayStoreTest {
    @Test
    void exactlyOneConcurrentReservationWins() {
        LocalWebhookReplayStore store = new LocalWebhookReplayStore();
        WebhookReplayKey key = key("delivery-race");
        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<CompletableFuture<ReserveOutcome>> attempts = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                attempts.add(CompletableFuture.supplyAsync(
                        () -> store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join(), executor));
            }
            CompletableFuture.allOf(attempts.toArray(CompletableFuture[]::new)).join();
            long reserved = attempts.stream().map(CompletableFuture::join)
                    .filter(ReserveOutcome.Reserved.class::isInstance).count();
            Assertions.assertEquals(1, reserved);
            Assertions.assertEquals(99, attempts.size() - reserved);
            Assertions.assertEquals(1, store.getSummary().entries());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void capacityFailsClosedAndExpiredEntriesRestoreCapacity() {
        AtomicLong clock = new AtomicLong(100);
        LocalWebhookReplayStore store = new LocalWebhookReplayStore(clock::get);
        store.setMaxEntries(1);
        store.reserve(key("first"), Duration.ofNanos(10)).toCompletableFuture().join();

        CompletionException failure = Assertions.assertThrows(CompletionException.class,
                () -> store.reserve(key("second"), Duration.ofMinutes(1)).toCompletableFuture().join());
        ReplayStoreUnavailableException unavailable =
                Assertions.assertInstanceOf(ReplayStoreUnavailableException.class, failure.getCause());
        Assertions.assertEquals(ReplayStoreUnavailableException.Reason.CAPACITY, unavailable.getReason());

        clock.set(111);
        Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                store.reserve(key("second"), Duration.ofMinutes(1)).toCompletableFuture().join());
    }

    @Test
    void ownerCheckedReleaseCannotDeleteNewerReservationAndForceRemovePermitsRedelivery() {
        LocalWebhookReplayStore store = new LocalWebhookReplayStore();
        WebhookReplayKey key = key("delivery-owner");
        ReplayReservation first = ((ReserveOutcome.Reserved) store.reserve(key, Duration.ofMinutes(1))
                .toCompletableFuture().join()).reservation();
        Assertions.assertTrue(store.forceRemove(key).toCompletableFuture().join());
        ReplayReservation second = ((ReserveOutcome.Reserved) store.reserve(key, Duration.ofMinutes(1))
                .toCompletableFuture().join()).reservation();

        store.release(first).toCompletableFuture().join();
        Assertions.assertSame(ReserveOutcome.Duplicate.INSTANCE,
                store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
        store.release(second).toCompletableFuture().join();
        Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
    }

    @Test
    void operationalRepresentationsAreRedactedAndServiceBindingSelectsLocalStore() {
        String selector = "sensitive-selector";
        String delivery = "sensitive-delivery";
        WebhookReplayKey key = new WebhookReplayKey("github", selector, delivery);
        ReplayReservation reservation = ReplayReservation.create(key);
        Assertions.assertFalse(key.toString().contains(selector));
        Assertions.assertFalse(key.toString().contains(delivery));
        Assertions.assertFalse(reservation.toString().contains(reservation.getOwnerToken()));

        WebhookReplayStore[] stores = com.networknt.service.SingletonServiceFactory.getBeans(WebhookReplayStore.class);
        Assertions.assertNotNull(stores);
        Assertions.assertEquals(1, stores.length);
        Assertions.assertInstanceOf(LocalWebhookReplayStore.class, stores[0]);
        Assertions.assertEquals(2, stores[0].getSummary().capacity());
    }

    @Test
    void newLocalStoreHasNoStateFromPriorProcessScope() {
        WebhookReplayKey key = key("restart-loss");
        LocalWebhookReplayStore firstProcess = new LocalWebhookReplayStore();
        firstProcess.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join();
        LocalWebhookReplayStore restartedProcess = new LocalWebhookReplayStore();
        Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                restartedProcess.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());
    }

    private WebhookReplayKey key(String delivery) {
        return new WebhookReplayKey("github", "shared", delivery);
    }
}
