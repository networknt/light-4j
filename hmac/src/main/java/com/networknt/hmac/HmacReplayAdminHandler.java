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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.AuditAttachmentUtil;
import com.networknt.handler.LightHttpHandler;
import com.networknt.hmac.config.HmacConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.status.HttpStatus;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Optional {@code POST /adm/hmac-replay/remove} handler. Applications must wire this exact route
 * behind their existing administrative authentication and authorization controls.
 */
public final class HmacReplayAdminHandler implements LightHttpHandler {
    public static final String INVALID_ADMIN_REQUEST = "ERR10096";
    private static final ObjectMapper MAPPER = Config.getInstance().getMapper();
    private final HmacRuntimeManager runtimeManager;
    private final Supplier<HmacConfig> hmacConfig;
    private final Supplier<UnifiedSecurityConfig> unifiedConfig;

    public HmacReplayAdminHandler() {
        this(new HmacRuntimeManager(), HmacConfig::load, UnifiedSecurityConfig::load);
    }

    HmacReplayAdminHandler(HmacRuntimeManager runtimeManager,
                           Supplier<HmacConfig> hmacConfig,
                           Supplier<UnifiedSecurityConfig> unifiedConfig) {
        this.runtimeManager = Objects.requireNonNull(runtimeManager);
        this.hmacConfig = Objects.requireNonNull(hmacConfig);
        this.unifiedConfig = Objects.requireNonNull(unifiedConfig);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Removal removal;
        try {
            removal = parse(exchange.getAttachment(AttachmentConstants.REQUEST_BODY));
        } catch (Exception e) {
            audit(exchange, "invalid", null);
            setExchangeStatus(exchange, INVALID_ADMIN_REQUEST);
            return;
        }

        HmacRuntime runtime;
        try {
            runtime = runtimeManager.get(hmacConfig.get(), unifiedConfig.get());
        } catch (Exception e) {
            audit(exchange, "unavailable", null);
            setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
            return;
        }
        HmacProfileRuntime profile = runtime.getProfile(removal.profile);
        String namespace = profile == null || !profile.isReplayEnabled()
                ? null : profile.normalizeReplaySelector(removal.selector);
        if (namespace == null) {
            audit(exchange, "invalid", runtime.getReplayStore());
            setExchangeStatus(exchange, INVALID_ADMIN_REQUEST);
            return;
        }
        removal = new Removal(removal.profile, namespace, removal.deliveryId);

        WebhookReplayStore store = runtime.getReplayStore();
        if (store == null) {
            audit(exchange, "unavailable", null);
            setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
            return;
        }
        WebhookReplayKey key = new WebhookReplayKey(removal.profile, removal.selector, removal.deliveryId);
        CompletionStage<Boolean> removalStage;
        try {
            removalStage = store.forceRemove(key);
            if (removalStage == null)
                throw new IllegalStateException("Replay store returned no completion stage.");
        } catch (Exception e) {
            audit(exchange, "unavailable", store);
            setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
            return;
        }
        exchange.dispatch();
        removalStage.whenComplete((removed, failure) -> exchange.dispatch(completed -> {
            if (failure != null) {
                audit(completed, "unavailable", store);
                setExchangeStatus(completed, HmacVerifier.HMAC_UNAVAILABLE);
                return;
            }
            audit(completed, Boolean.TRUE.equals(removed) ? "removed" : "not_found", store);
            completed.setStatusCode(HttpStatus.OK.value());
            completed.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("removed", Boolean.TRUE.equals(removed));
            response.put("scope", store.getScope().getValue());
            try {
                completed.getResponseSender().send(MAPPER.writeValueAsString(response));
            } catch (Exception e) {
                setExchangeStatus(completed, HmacVerifier.HMAC_UNAVAILABLE);
            }
        }));
    }

    private Removal parse(Object requestBody) {
        if (!(requestBody instanceof Map<?, ?> body))
            throw new IllegalArgumentException("Request body must be an object.");
        return new Removal(text(body.get("profile")), optionalText(body.get("selector")), text(body.get("deliveryId")));
    }

    private String text(Object value) {
        if (!(value instanceof String text) || text.isBlank())
            throw new IllegalArgumentException("Required field is missing.");
        return text;
    }

    private String optionalText(Object value) {
        if (value == null)
            return null;
        if (!(value instanceof String text))
            throw new IllegalArgumentException("Selector must be a string.");
        return text;
    }

    private void audit(HttpServerExchange exchange, String outcome, WebhookReplayStore store) {
        AuditAttachmentUtil.populateAuditAttachmentField(exchange, "hmacReplayAdminAction", "forceRemove");
        AuditAttachmentUtil.populateAuditAttachmentField(exchange, "hmacReplayAdminOutcome", outcome);
        if (store != null)
            AuditAttachmentUtil.populateAuditAttachmentField(exchange, "hmacReplayStoreScope", store.getScope().getValue());
    }

    private record Removal(String profile, String selector, String deliveryId) {
    }
}
