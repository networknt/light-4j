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

import java.util.Arrays;

/** Immutable, secret-free result of successful raw-body HMAC verification. */
public final class HmacPendingAuthentication {
    private final String matchedPrefix;
    private final String profileName;
    private final String selectorNamespace;
    private final String replayId;
    private final String originalPath;
    private final long bodyLength;
    private final byte[] bodyFingerprint;
    private final HmacRuntime runtime;

    HmacPendingAuthentication(String matchedPrefix,
                              String profileName,
                              String selectorNamespace,
                              String replayId,
                              String originalPath,
                              long bodyLength,
                              byte[] bodyFingerprint,
                              HmacRuntime runtime) {
        this.matchedPrefix = matchedPrefix;
        this.profileName = profileName;
        this.selectorNamespace = selectorNamespace;
        this.replayId = replayId;
        this.originalPath = originalPath;
        this.bodyLength = bodyLength;
        this.bodyFingerprint = Arrays.copyOf(bodyFingerprint, bodyFingerprint.length);
        this.runtime = runtime;
    }

    public String getMatchedPrefix() {
        return matchedPrefix;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getSelectorNamespace() {
        return selectorNamespace;
    }

    public String getReplayId() {
        return replayId;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public long getBodyLength() {
        return bodyLength;
    }

    public byte[] getBodyFingerprint() {
        return Arrays.copyOf(bodyFingerprint, bodyFingerprint.length);
    }

    public HmacRuntime getRuntime() {
        return runtime;
    }

    @Override
    public String toString() {
        return "HmacPendingAuthentication{matchedPrefix='" + matchedPrefix
                + "', profileName='" + profileName
                + "', bodyLength=" + bodyLength + ", sensitiveFields=[REDACTED]}";
    }
}
