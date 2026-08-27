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

import com.networknt.config.ConfigException;
import com.networknt.handler.Handler;
import com.networknt.handler.HandlerChainValidator;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptor;
import com.networknt.hmac.config.HmacConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Asynchronous replay gate between request interception and Unified Security. */
public final class HmacHandler implements MiddlewareHandler, HandlerChainValidator {
    public static final String BODY_CHANGED = "ERR10097";
    private static final Logger LOG = LoggerFactory.getLogger(HmacHandler.class);
    private static final String UNIFIED_SECURITY_HANDLER = "com.networknt.security.UnifiedSecurityHandler";

    private final Supplier<HmacConfig> hmacConfig;
    private final Supplier<UnifiedSecurityConfig> unifiedConfig;
    private final boolean validateNext;
    private volatile HttpHandler next;

    public HmacHandler() {
        this(HmacConfig::load, UnifiedSecurityConfig::load, true);
        validateInterceptorOrder();
    }

    HmacHandler(Supplier<HmacConfig> hmacConfig,
                Supplier<UnifiedSecurityConfig> unifiedConfig,
                boolean validateNext) {
        this.hmacConfig = Objects.requireNonNull(hmacConfig);
        this.unifiedConfig = Objects.requireNonNull(unifiedConfig);
        this.validateNext = validateNext;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        HmacPendingAuthentication pending = exchange.getAttachment(HmacAttachments.PENDING_AUTHENTICATION);
        UnifiedPathPrefixAuth rule = firstMatch(exchange.getRequestPath(), unifiedConfig.get());
        if (rule == null || rule.getHmacProfile() == null) {
            if (pending != null) {
                HmacMetrics.request(pending.getProfileName(), "chain_error");
                LOG.error("HMAC request path or rule changed after verification for profile={} prefix={}",
                        pending.getProfileName(), pending.getMatchedPrefix());
                setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
                return;
            }
            Handler.next(exchange, next);
            return;
        }

        if (!matches(pending, rule, exchange.getRequestPath())) {
            chainError(exchange, rule, "missing_or_mismatched_pending_evidence");
            return;
        }
        BodyFingerprint current = fingerprint(exchange);
        if (current == null) {
            chainError(exchange, rule, "missing_buffered_body");
            return;
        }
        if (current.length != pending.getBodyLength()
                || !MessageDigest.isEqual(current.sha256, pending.getBodyFingerprint())) {
            HmacMetrics.request(rule.getHmacProfile(), "chain_error");
            LOG.error("HMAC buffered body changed after verification for profile={} prefix={}",
                    rule.getHmacProfile(), rule.getPrefix());
            setExchangeStatus(exchange, BODY_CHANGED);
            return;
        }

        HmacProfileRuntime profile = pending.getRuntime().getProfile(pending.getProfileName());
        if (profile == null) {
            chainError(exchange, rule, "pinned_profile_unavailable");
            return;
        }
        if (!profile.isReplayEnabled()) {
            admit(exchange, pending);
            HmacMetrics.request(profile.getName(), "accepted");
            Handler.next(exchange, next);
            return;
        }

        WebhookReplayStore store = pending.getRuntime().getReplayStore();
        if (store == null) {
            unavailable(exchange, rule, null, "store_missing");
            return;
        }
        CompletionStage<ReserveOutcome> stage;
        try {
            WebhookReplayKey key = new WebhookReplayKey(pending.getProfileName(),
                    pending.getSelectorNamespace(), pending.getReplayId());
            stage = store.reserve(key, profile.getReplayRetention());
            if (stage == null)
                throw new IllegalStateException("Replay store returned no completion stage.");
        } catch (Exception e) {
            unavailable(exchange, rule, store, "reserve_start_failed");
            return;
        }

        exchange.dispatch();
        stage.whenComplete((outcome, failure) -> exchange.dispatch(completed -> {
            if (failure != null || outcome == null) {
                unavailable(completed, rule, store, "reserve_failed");
            } else if (outcome instanceof ReserveOutcome.Duplicate) {
                HmacMetrics.replay(store, "reserve", "duplicate");
                HmacMetrics.request(profile.getName(), "duplicate");
                LOG.info("HMAC replay duplicate suppressed for profile={} prefix={} store_type={}",
                        profile.getName(), rule.getPrefix(), store.getScope().getValue());
                completed.setStatusCode(StatusCodes.OK);
                completed.endExchange();
            } else if (outcome instanceof ReserveOutcome.Reserved reserved) {
                HmacMetrics.replay(store, "reserve", "reserved");
                HmacMetrics.request(profile.getName(), "accepted");
                admit(completed, pending);
                addReservationCompletionListener(completed, store, reserved.reservation(),
                        profile.getName(), rule.getPrefix());
                try {
                    Handler.next(completed, next);
                } catch (Exception e) {
                    LOG.error("HMAC admitted chain failed for profile={} prefix={}",
                            profile.getName(), rule.getPrefix());
                    throw e;
                }
            } else {
                unavailable(completed, rule, store, "unknown_reserve_outcome");
            }
        }));
    }

    private void addReservationCompletionListener(HttpServerExchange exchange,
                                                   WebhookReplayStore store,
                                                   ReplayReservation reservation,
                                                   String profile,
                                                   String prefix) {
        exchange.addExchangeCompleteListener((completed, nextListener) -> {
            try {
                int status = completed.getStatusCode();
                if (status < 200 || status >= 300) {
                    CompletionStage<Void> release = store.release(reservation);
                    if (release == null) {
                        HmacMetrics.replay(store, "release", "failed");
                        LOG.error("HMAC replay release returned no stage for profile={} prefix={} final_status={}",
                                profile, prefix, status);
                    } else {
                        release.whenComplete((ignored, failure) -> {
                            HmacMetrics.replay(store, "release", failure == null ? "released" : "failed");
                            if (failure != null)
                                LOG.error("HMAC replay release failed for profile={} prefix={} final_status={} store_type={}",
                                        profile, prefix, status, store.getScope().getValue());
                        });
                    }
                } else {
                    HmacMetrics.replay(store, "retain", "retained");
                }
            } catch (Exception e) {
                HmacMetrics.replay(store, "release", "failed");
                LOG.error("HMAC replay completion handling failed for profile={} prefix={} final_status={}",
                        profile, prefix, completed.getStatusCode());
            } finally {
                nextListener.proceed();
            }
        });
    }

    private void admit(HttpServerExchange exchange, HmacPendingAuthentication pending) {
        exchange.putAttachment(HmacAttachments.AUTHENTICATION_EVIDENCE,
                new HmacAuthenticationEvidence(pending));
    }

    private boolean matches(HmacPendingAuthentication pending,
                            UnifiedPathPrefixAuth rule,
                            String requestPath) {
        return pending != null
                && pending.getRuntime() != null
                && rule.getPrefix().equals(pending.getMatchedPrefix())
                && rule.getHmacProfile().equals(pending.getProfileName())
                && requestPath.equals(pending.getOriginalPath());
    }

    private BodyFingerprint fingerprint(HttpServerExchange exchange) {
        PooledByteBuffer[] pooled = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        if (pooled == null) {
            if (exchange.isRequestComplete() && exchange.getRequestContentLength() == 0)
                return new BodyFingerprint(0, sha256(null));
            return null;
        }
        long length = 0;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (PooledByteBuffer pooledBuffer : pooled) {
                if (pooledBuffer != null) {
                    ByteBuffer duplicate = pooledBuffer.getBuffer().duplicate();
                    length += duplicate.remaining();
                    digest.update(duplicate);
                }
            }
            return new BodyFingerprint(length, digest.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes == null ? new byte[0] : bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
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

    private void chainError(HttpServerExchange exchange, UnifiedPathPrefixAuth rule, String outcome) {
        HmacMetrics.request(rule.getHmacProfile(), "chain_error");
        LOG.error("HMAC chain validation failed for profile={} prefix={} outcome={}",
                rule.getHmacProfile(), rule.getPrefix(), outcome);
        setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
    }

    private void unavailable(HttpServerExchange exchange,
                             UnifiedPathPrefixAuth rule,
                             WebhookReplayStore store,
                             String outcome) {
        HmacMetrics.replay(store, "reserve", "unavailable");
        HmacMetrics.request(rule.getHmacProfile(), "store_unavailable");
        LOG.error("HMAC replay unavailable for profile={} prefix={} outcome={} store_type={}",
                rule.getHmacProfile(), rule.getPrefix(), outcome,
                store == null ? "unavailable" : store.getScope().getValue());
        setExchangeStatus(exchange, HmacVerifier.HMAC_UNAVAILABLE);
    }

    private void validateInterceptorOrder() {
        RequestInterceptor[] interceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        HmacIntegrationValidator.validateInterceptorOrder(interceptors);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        if (validateNext && !UNIFIED_SECURITY_HANDLER.equals(next.getClass().getName()))
            throw new ConfigException("HmacHandler must be immediately before UnifiedSecurityHandler in handler.yml.");
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return hmacConfig.get().isEnabled();
    }

    @Override
    public void validateChain(List<HttpHandler> chain, int index, String location) {
        if (index + 1 >= chain.size()
                || !UNIFIED_SECURITY_HANDLER.equals(chain.get(index + 1).getClass().getName()))
            throw new ConfigException("HmacHandler must be immediately before UnifiedSecurityHandler in handler.yml "
                    + location + '.');
    }

    private record BodyFingerprint(long length, byte[] sha256) {
        private BodyFingerprint {
            sha256 = Arrays.copyOf(sha256, sha256.length);
        }
    }
}
