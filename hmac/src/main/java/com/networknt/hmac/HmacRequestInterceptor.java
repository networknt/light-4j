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
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInjectionConfig;
import com.networknt.handler.RequestInterceptor;
import com.networknt.reqtrans.RequestTransformerConfig;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.server.ServerConfig;
import com.networknt.status.Status;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/** First request interceptor that verifies configured raw-body HMAC profiles. */
public final class HmacRequestInterceptor implements RequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(HmacRequestInterceptor.class);
    private final HmacRuntimeManager runtimeManager;
    private final Supplier<HmacConfig> hmacConfig;
    private final Supplier<UnifiedSecurityConfig> unifiedConfig;
    private final Supplier<RequestInjectionConfig> requestInjectionConfig;
    private final Supplier<ServerConfig> serverConfig;
    private final Supplier<RequestTransformerConfig> requestTransformerConfig;
    private final HmacVerifier verifier = new HmacVerifier();
    private volatile ValidatedIntegration validatedIntegration;
    private volatile HttpHandler next;

    public HmacRequestInterceptor() {
        this(new HmacRuntimeManager(), HmacConfig::load, UnifiedSecurityConfig::load,
                RequestInjectionConfig::load, ServerConfig::load, RequestTransformerConfig::load);
        HmacConfig initial = hmacConfig.get();
        UnifiedSecurityConfig initialUnified = unifiedConfig.get();
        HmacRuntime initialRuntime = runtimeManager.get(initial, initialUnified);
        validateIntegration(initialRuntime, initialUnified);
    }

    HmacRequestInterceptor(HmacRuntimeManager runtimeManager,
                           Supplier<HmacConfig> hmacConfig,
                           Supplier<UnifiedSecurityConfig> unifiedConfig,
                           Supplier<RequestInjectionConfig> requestInjectionConfig,
                           Supplier<ServerConfig> serverConfig) {
        this(runtimeManager, hmacConfig, unifiedConfig, requestInjectionConfig, serverConfig,
                RequestTransformerConfig::load);
    }

    HmacRequestInterceptor(HmacRuntimeManager runtimeManager,
                           Supplier<HmacConfig> hmacConfig,
                           Supplier<UnifiedSecurityConfig> unifiedConfig,
                           Supplier<RequestInjectionConfig> requestInjectionConfig,
                           Supplier<ServerConfig> serverConfig,
                           Supplier<RequestTransformerConfig> requestTransformerConfig) {
        this.runtimeManager = Objects.requireNonNull(runtimeManager);
        this.hmacConfig = Objects.requireNonNull(hmacConfig);
        this.unifiedConfig = Objects.requireNonNull(unifiedConfig);
        this.requestInjectionConfig = Objects.requireNonNull(requestInjectionConfig);
        this.serverConfig = Objects.requireNonNull(serverConfig);
        this.requestTransformerConfig = Objects.requireNonNull(requestTransformerConfig);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        long started = System.nanoTime();
        String profileName = null;
        try {
            HmacConfig currentHmac = hmacConfig.get();
            UnifiedSecurityConfig currentUnified = unifiedConfig.get();
            UnifiedPathPrefixAuth rule = firstMatch(exchange.getRequestPath(), currentUnified);
            profileName = rule == null ? null : rule.getHmacProfile();
            HmacRuntime runtime = runtimeManager.get(currentHmac, currentUnified);
            if (!currentHmac.isEnabled())
                return;
            validateIntegration(runtime, currentUnified);
            Status status = verifier.verify(exchange, runtime, currentUnified);
            HmacPendingAuthentication pending = exchange.getAttachment(HmacAttachments.PENDING_AUTHENTICATION);
            if (profileName != null)
                HmacMetrics.verification(profileName, System.nanoTime() - started,
                        pending == null ? -1 : pending.getBodyLength());
            if (status != null) {
                HmacMetrics.request(profileName, outcome(status));
                setExchangeStatus(exchange, status);
            }
        } catch (Exception e) {
            HmacMetrics.request(profileName, "runtime_error");
            LOG.error("HMAC runtime or verification failed closed for profile={}",
                    profileName == null ? "unknown" : profileName, e);
            setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
        }
    }

    private void validateIntegration(HmacRuntime runtime, UnifiedSecurityConfig unified) {
        RequestInjectionConfig injection = requestInjectionConfig.get();
        ServerConfig server = serverConfig.get();
        RequestTransformerConfig transformer = requestTransformerConfig.get();
        ValidatedIntegration current = validatedIntegration;
        if (current != null && current.matches(runtime, unified, injection, server, transformer))
            return;
        synchronized (this) {
            current = validatedIntegration;
            if (current == null || !current.matches(runtime, unified, injection, server, transformer)) {
                HmacIntegrationValidator.validate(runtime, unified, injection, server, transformer);
                validatedIntegration = new ValidatedIntegration(runtime, unified, injection, server, transformer);
            }
        }
    }

    private UnifiedPathPrefixAuth firstMatch(String requestPath, UnifiedSecurityConfig config) {
        if (config.getPathPrefixAuths() != null) {
            for (UnifiedPathPrefixAuth rule : config.getPathPrefixAuths()) {
                if (rule.getPrefix() != null && requestPath.startsWith(rule.getPrefix()))
                    return rule;
            }
        }
        return null;
    }

    private String outcome(Status status) {
        return switch (status.getStatusCode()) {
            case 413 -> "too_large";
            case 415 -> "unsupported_encoding";
            case 503 -> "store_unavailable";
            default -> "invalid";
        };
    }

    private record ValidatedIntegration(HmacRuntime runtime,
                                        UnifiedSecurityConfig unified,
                                        RequestInjectionConfig injection,
                                        ServerConfig server,
                                        RequestTransformerConfig transformer) {
        private boolean matches(HmacRuntime currentRuntime,
                                UnifiedSecurityConfig currentUnified,
                                RequestInjectionConfig currentInjection,
                                ServerConfig currentServer,
                                RequestTransformerConfig currentTransformer) {
            return runtime == currentRuntime && unified == currentUnified && injection == currentInjection
                    && server == currentServer && transformer == currentTransformer;
        }
    }

    @Override
    public boolean isRequiredContent() {
        return hmacConfig.get().isEnabled();
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return hmacConfig.get().isEnabled();
    }
}
