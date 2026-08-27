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

import java.util.Objects;
import java.util.UUID;

/** Opaque ownership proof required for normal release. */
public final class ReplayReservation {
    private final WebhookReplayKey key;
    private final String ownerToken;

    private ReplayReservation(WebhookReplayKey key, String ownerToken) {
        this.key = Objects.requireNonNull(key);
        this.ownerToken = Objects.requireNonNull(ownerToken);
    }

    public static ReplayReservation create(WebhookReplayKey key) {
        return new ReplayReservation(key, UUID.randomUUID().toString());
    }

    public WebhookReplayKey getKey() {
        return key;
    }

    /** Intended only for replay-store provider implementations. */
    public String getOwnerToken() {
        return ownerToken;
    }

    @Override
    public String toString() {
        return "ReplayReservation{key=" + key + ", ownerToken=[REDACTED]}";
    }
}
