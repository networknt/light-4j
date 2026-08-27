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

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/** Atomic asynchronous replay reservation contract selected through {@code service.yml}. */
public interface WebhookReplayStore {
    CompletionStage<ReserveOutcome> reserve(WebhookReplayKey key, Duration retention);

    CompletionStage<Void> release(ReplayReservation reservation);

    CompletionStage<Boolean> forceRemove(WebhookReplayKey key);

    ReplayStoreScope getScope();

    ReplayStoreSummary getSummary();

    /** Validate provider configuration without substituting another implementation. */
    default void validate() {
    }
}
