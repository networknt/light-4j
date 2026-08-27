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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Logical replay identity. Its printable and persistent representations are one-way hashes. */
public final class WebhookReplayKey {
    private final String digest;

    public WebhookReplayKey(String profile, String selectorNamespace, String deliveryId) {
        this.digest = digest(requireText(profile, "profile"),
                requireText(selectorNamespace, "selectorNamespace"),
                requireText(deliveryId, "deliveryId"));
    }

    public String getDigest() {
        return digest;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank.");
        return value;
    }

    private static String digest(String profile, String selector, String deliveryId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, profile);
            update(digest, selector);
            update(digest, deliveryId);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof WebhookReplayKey key))
            return false;
        return digest.equals(key.digest);
    }

    @Override
    public int hashCode() {
        return digest.hashCode();
    }

    @Override
    public String toString() {
        return "WebhookReplayKey{digest=" + digest.substring(0, 12) + "...}";
    }
}
