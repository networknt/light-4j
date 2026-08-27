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

import com.networknt.metrics.AbstractMetricsHandler;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.MetricName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Bounded-cardinality HMAC metrics; selector and delivery identifiers are never labels. */
final class HmacMetrics {
    private HmacMetrics() {
    }

    static void request(String profile, String outcome) {
        AbstractMetricsHandler.registry.counter(metric("hmac_webhook_requests_total",
                Map.of("profile", safe(profile), "outcome", outcome))).inc();
    }

    static void verification(String profile, long elapsedNanos, long bodyBytes) {
        String safeProfile = safe(profile);
        AbstractMetricsHandler.registry.timer(metric("hmac_webhook_verification_duration_seconds",
                Map.of("profile", safeProfile))).update(elapsedNanos, TimeUnit.NANOSECONDS);
        if (bodyBytes >= 0)
            AbstractMetricsHandler.registry.histogram(metric("hmac_webhook_body_bytes",
                    Map.of("profile", safeProfile))).update(bodyBytes);
    }

    static void replay(WebhookReplayStore store, String operation, String outcome) {
        String storeType = store == null ? "unavailable" : store.getScope().getValue();
        AbstractMetricsHandler.registry.counter(metric("hmac_replay_operations_total",
                Map.of("store_type", storeType, "operation", operation, "outcome", outcome))).inc();
        if (store != null && store.getScope() == ReplayStoreScope.LOCAL)
            registerLocalGauge(store);
    }

    private static void registerLocalGauge(WebhookReplayStore store) {
        MetricName name = metric("hmac_replay_local_entries", Map.of("store_type", "local"));
        if (!AbstractMetricsHandler.registry.getMetrics().containsKey(name)) {
            try {
                AbstractMetricsHandler.registry.register(name,
                        (Gauge<Long>) () -> store.getSummary().entries());
            } catch (IllegalArgumentException ignored) {
                // Another request registered the process-wide gauge concurrently.
            }
        }
    }

    private static MetricName metric(String name, Map<String, String> tags) {
        return new MetricName(name).tagged(tags);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
