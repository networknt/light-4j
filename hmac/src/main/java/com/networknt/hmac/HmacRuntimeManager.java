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

import com.networknt.hmac.config.HmacConfig;
import com.networknt.security.UnifiedSecurityConfig;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Atomically replaces compiled runtimes when either source config snapshot changes. */
public final class HmacRuntimeManager {
    private final Function<String, String> environment;
    private final Supplier<WebhookReplayStore[]> replayStores;
    private volatile WebhookReplayStore[] replayStoreBinding;
    private volatile Snapshot snapshot;

    public HmacRuntimeManager() {
        this(System::getenv, () -> com.networknt.service.SingletonServiceFactory.getBeans(WebhookReplayStore.class));
    }

    public HmacRuntimeManager(Function<String, String> environment) {
        this(environment, () -> com.networknt.service.SingletonServiceFactory.getBeans(WebhookReplayStore.class));
    }

    public HmacRuntimeManager(Function<String, String> environment,
                              Supplier<WebhookReplayStore[]> replayStores) {
        this.environment = Objects.requireNonNull(environment);
        this.replayStores = Objects.requireNonNull(replayStores);
    }

    public HmacRuntime get(HmacConfig hmacConfig, UnifiedSecurityConfig unifiedConfig) {
        Snapshot current = snapshot;
        if (current != null && current.hmacConfig == hmacConfig && current.unifiedConfig == unifiedConfig)
            return current.runtime;
        synchronized (this) {
            current = snapshot;
            if (current != null && current.hmacConfig == hmacConfig && current.unifiedConfig == unifiedConfig)
                return current.runtime;
            HmacRuntime replacement = HmacRuntime.compile(hmacConfig, unifiedConfig, environment, this::getReplayStores);
            snapshot = new Snapshot(hmacConfig, unifiedConfig, replacement);
            return replacement;
        }
    }

    private WebhookReplayStore[] getReplayStores() {
        WebhookReplayStore[] current = replayStoreBinding;
        if (current != null)
            return current.clone();
        synchronized (this) {
            current = replayStoreBinding;
            if (current == null) {
                WebhookReplayStore[] resolved = replayStores.get();
                current = resolved == null ? new WebhookReplayStore[0] : resolved.clone();
                replayStoreBinding = current;
            }
            return current.clone();
        }
    }

    private record Snapshot(HmacConfig hmacConfig,
                            UnifiedSecurityConfig unifiedConfig,
                            HmacRuntime runtime) {
    }
}
