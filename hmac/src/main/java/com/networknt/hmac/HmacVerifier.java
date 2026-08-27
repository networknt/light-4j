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

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.status.Status;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** CPU-only raw-body HMAC verifier used by {@link HmacRequestInterceptor}. */
public final class HmacVerifier {
    public static final String INVALID_HMAC = "ERR10093";
    public static final String UNSUPPORTED_CONTENT_ENCODING = "ERR10094";
    public static final String HMAC_UNAVAILABLE = "ERR10095";
    public static final String METHOD_NOT_ALLOWED = "ERR10008";
    public static final String PAYLOAD_TOO_LARGE = "ERR10068";

    public Status verify(HttpServerExchange exchange,
                         HmacRuntime runtime,
                         UnifiedSecurityConfig unifiedConfig) {
        UnifiedPathPrefixAuth rule = firstMatch(exchange.getRequestPath(), unifiedConfig);
        if (rule == null || rule.getHmacProfile() == null)
            return null;

        HmacProfileRuntime profile = runtime.getProfile(rule.getHmacProfile());
        if (profile == null)
            return new Status(HMAC_UNAVAILABLE);
        String method = exchange.getRequestMethod().toString().toUpperCase(Locale.ROOT);
        if (!profile.getAllowedMethods().contains(method))
            return new Status(METHOD_NOT_ALLOWED, method, exchange.getRequestPath());
        if (!hasIdentityEncoding(exchange))
            return new Status(UNSUPPORTED_CONTENT_ENCODING);

        Body body = readBody(exchange);
        if (body == null)
            return new Status(HMAC_UNAVAILABLE);
        if (body.length > profile.getMaxBodyBytes())
            return new Status(PAYLOAD_TOO_LARGE);

        SingleHeader signature = singleHeader(exchange, profile.getSignatureHeader(), false);
        byte[] suppliedSignature = signature.valid ? decodeSignature(signature.value, profile) : null;
        if (suppliedSignature == null)
            return new Status(INVALID_HMAC);

        String selector = null;
        if (!profile.getSelectorHeader().isBlank()) {
            SingleHeader selectorHeader = singleHeader(exchange, profile.getSelectorHeader(), true);
            if (!selectorHeader.valid)
                return new Status(INVALID_HMAC);
            selector = selectorHeader.value;
        }

        String selectorNamespace;
        try {
            selectorNamespace = profile.verify(selector, suppliedSignature, body.buffers);
        } catch (Exception e) {
            return new Status(HMAC_UNAVAILABLE);
        }
        if (selectorNamespace == null)
            return new Status(INVALID_HMAC);

        String replayId = null;
        if (profile.isReplayEnabled()) {
            SingleHeader replayHeader = singleHeader(exchange, profile.getReplayIdHeader(), false);
            if (!replayHeader.valid || replayHeader.value.isEmpty())
                return new Status(INVALID_HMAC);
            replayId = replayHeader.value;
        }

        exchange.putAttachment(HmacAttachments.PENDING_AUTHENTICATION,
                new HmacPendingAuthentication(rule.getPrefix(), profile.getName(), selectorNamespace,
                        replayId, exchange.getRequestPath(), body.length, body.fingerprint, runtime));
        return null;
    }

    private UnifiedPathPrefixAuth firstMatch(String requestPath, UnifiedSecurityConfig config) {
        if (config.getPathPrefixAuths() == null)
            return null;
        for (UnifiedPathPrefixAuth rule : config.getPathPrefixAuths()) {
            if (rule.getPrefix() != null && requestPath.startsWith(rule.getPrefix()))
                return rule;
        }
        return null;
    }

    private boolean hasIdentityEncoding(HttpServerExchange exchange) {
        HeaderValues values = exchange.getRequestHeaders().get(Headers.CONTENT_ENCODING);
        return values == null || (values.size() == 1 && "identity".equalsIgnoreCase(trimOws(values.getFirst())));
    }

    private Body readBody(HttpServerExchange exchange) {
        PooledByteBuffer[] pooled = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        if (pooled == null) {
            if (exchange.isRequestComplete() && exchange.getRequestContentLength() == 0)
                return new Body(List.of(), 0, sha256(List.of()));
            return null;
        }
        List<ByteBuffer> buffers = new ArrayList<>();
        long length = 0;
        for (PooledByteBuffer buffer : pooled) {
            if (buffer != null) {
                ByteBuffer duplicate = buffer.getBuffer().duplicate();
                length += duplicate.remaining();
                buffers.add(duplicate);
            }
        }
        return new Body(List.copyOf(buffers), length, sha256(buffers));
    }

    private byte[] sha256(List<ByteBuffer> buffers) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ByteBuffer buffer : buffers)
                digest.update(buffer.duplicate());
            return digest.digest();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private SingleHeader singleHeader(HttpServerExchange exchange, String header, boolean optional) {
        HeaderValues values = exchange.getRequestHeaders().get(new HttpString(header));
        if (values == null)
            return new SingleHeader(optional, null);
        if (values.size() != 1)
            return new SingleHeader(false, null);
        return new SingleHeader(true, trimOws(values.getFirst()));
    }

    private byte[] decodeSignature(String value, HmacProfileRuntime profile) {
        if (value == null || !value.startsWith(profile.getSignaturePrefix()))
            return null;
        String encoded = value.substring(profile.getSignaturePrefix().length());
        try {
            byte[] decoded = "hex".equals(profile.getSignatureEncoding())
                    ? HexFormat.of().parseHex(encoded)
                    : Base64.getDecoder().decode(encoded);
            return decoded.length == 32 ? decoded : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String trimOws(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && (value.charAt(start) == ' ' || value.charAt(start) == '\t'))
            start++;
        while (end > start && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t'))
            end--;
        return value.substring(start, end);
    }

    private record Body(List<ByteBuffer> buffers, long length, byte[] fingerprint) {
    }

    private record SingleHeader(boolean valid, String value) {
    }
}
