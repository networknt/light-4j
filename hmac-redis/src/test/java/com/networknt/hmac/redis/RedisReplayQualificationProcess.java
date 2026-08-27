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

import com.networknt.hmac.ReserveOutcome;
import com.networknt.hmac.WebhookReplayKey;

import java.time.Duration;

/** Separate-JVM probe used by the Phase 4 Redis qualification gate. */
public final class RedisReplayQualificationProcess {
    private RedisReplayQualificationProcess() {
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException("Expected Redis URL, key prefix, delivery ID, and retention seconds.");
        }

        RedisWebhookReplayStore store = new RedisWebhookReplayStore(name -> args[0], null);
        store.setKeyPrefix(args[1]);
        try {
            store.validate();
            ReserveOutcome outcome = store.reserve(
                    new WebhookReplayKey("github", "phase4-shared-selector", args[2]),
                    Duration.ofSeconds(Long.parseLong(args[3]))).toCompletableFuture().join();
            System.out.println("HMAC_PHASE4_OUTCOME="
                    + (outcome instanceof ReserveOutcome.Reserved ? "RESERVED" : "DUPLICATE"));
        } finally {
            store.onShutdown();
        }
    }
}
