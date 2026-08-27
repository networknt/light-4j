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

/** Opaque, secret-free proof that verification and replay admission both completed. */
public final class HmacAuthenticationEvidence {
    private final String matchedPrefix;
    private final String profileName;
    private final String originalPath;
    private final HmacRuntime runtime;

    HmacAuthenticationEvidence(HmacPendingAuthentication pending) {
        this.matchedPrefix = pending.getMatchedPrefix();
        this.profileName = pending.getProfileName();
        this.originalPath = pending.getOriginalPath();
        this.runtime = pending.getRuntime();
    }

    public boolean matches(String prefix, String profile, String requestPath) {
        return runtime != null
                && matchedPrefix.equals(prefix)
                && profileName.equals(profile)
                && originalPath.equals(requestPath);
    }

    @Override
    public String toString() {
        return "HmacAuthenticationEvidence{matchedPrefix='" + matchedPrefix
                + "', profileName='" + profileName + "', sensitiveFields=[REDACTED]}";
    }
}
