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
import com.networknt.hmac.config.HmacConfig;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.service.SingletonServiceFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Immutable runtime snapshot pinned to requests that pass HMAC verification. */
public final class HmacRuntime {
    private final Map<String, HmacProfileRuntime> profiles;
    private final WebhookReplayStore replayStore;

    private HmacRuntime(Map<String, HmacProfileRuntime> profiles, WebhookReplayStore replayStore) {
        this.profiles = Collections.unmodifiableMap(profiles);
        this.replayStore = replayStore;
    }

    public static HmacRuntime compile(HmacConfig config,
                                      UnifiedSecurityConfig unifiedConfig,
                                      Function<String, String> environment) {
        return compile(config, unifiedConfig, environment,
                () -> SingletonServiceFactory.getBeans(WebhookReplayStore.class));
    }

    static HmacRuntime compile(HmacConfig config,
                               UnifiedSecurityConfig unifiedConfig,
                               Function<String, String> environment,
                               Supplier<WebhookReplayStore[]> replayStores) {
        if (!config.isEnabled()) {
            HmacRuntime disabled = new HmacRuntime(Collections.emptyMap(), null);
            disabled.validateUnifiedSecurity(unifiedConfig);
            return disabled;
        }
        Map<String, HmacProfileRuntime> compiled = new LinkedHashMap<>();
        config.getProfiles().forEach((name, profile) ->
                compiled.put(name, new HmacProfileRuntime(name, profile, environment)));
        WebhookReplayStore replayStore = resolveReplayStore(compiled, replayStores);
        HmacRuntime runtime = new HmacRuntime(compiled, replayStore);
        runtime.validateUnifiedSecurity(unifiedConfig);
        return runtime;
    }

    private static WebhookReplayStore resolveReplayStore(Map<String, HmacProfileRuntime> profiles,
                                                          Supplier<WebhookReplayStore[]> replayStores) {
        if (profiles.values().stream().noneMatch(HmacProfileRuntime::isReplayEnabled))
            return null;
        WebhookReplayStore[] stores;
        try {
            stores = replayStores.get();
        } catch (Exception e) {
            throw new ConfigException("Unable to resolve WebhookReplayStore from service.yml.");
        }
        if (stores == null || stores.length != 1 || stores[0] == null)
            throw new ConfigException("Exactly one WebhookReplayStore must be bound in service.yml when replay is enabled.");
        try {
            stores[0].validate();
        } catch (Exception e) {
            throw new ConfigException("The configured WebhookReplayStore is invalid.");
        }
        return stores[0];
    }

    public HmacProfileRuntime getProfile(String name) {
        return profiles.get(name);
    }

    public Map<String, HmacProfileRuntime> getProfiles() {
        return profiles;
    }

    public WebhookReplayStore getReplayStore() {
        return replayStore;
    }

    private void validateUnifiedSecurity(UnifiedSecurityConfig config) {
        List<UnifiedPathPrefixAuth> rules = config.getPathPrefixAuths();
        if (rules == null)
            return;
        for (int index = 0; index < rules.size(); index++) {
            UnifiedPathPrefixAuth rule = rules.get(index);
            String profileName = rule.getHmacProfile();
            if (profileName == null)
                continue;
            String path = "unified-security.pathPrefixAuths[" + index + ']';
            if (profileName.isBlank())
                throw new ConfigException(path + ".hmacProfile must not be blank.");
            if (!profiles.containsKey(profileName))
                throw new ConfigException(path + ".hmacProfile references unknown profile " + profileName + '.');
            if (rule.getPrefix() == null || rule.getPrefix().isBlank())
                throw new ConfigException(path + ".prefix must not be blank for an HMAC rule.");
            if (rule.isBasic() || rule.isSjwt() || rule.isSwt())
                throw new ConfigException(path + " combines HMAC with an unsupported authentication factor.");
            if (rule.isJwt() && rule.isApikey())
                throw new ConfigException(path + " cannot combine HMAC with both JWT and API key.");

            for (int earlier = 0; earlier < index; earlier++) {
                String earlierPrefix = rules.get(earlier).getPrefix();
                if (earlierPrefix != null && (rule.getPrefix().startsWith(earlierPrefix)
                        || earlierPrefix.startsWith(rule.getPrefix())))
                    throw new ConfigException(path + " is shadowed by earlier prefix " + earlierPrefix + '.');
            }
            if (config.getAnonymousPrefixes() != null) {
                for (String anonymous : config.getAnonymousPrefixes()) {
                    if (anonymous != null && (rule.getPrefix().startsWith(anonymous)
                            || anonymous.startsWith(rule.getPrefix())))
                        throw new ConfigException(path + " overlaps anonymous prefix " + anonymous + '.');
                }
            }
        }
    }

    @Override
    public String toString() {
        return "HmacRuntime{profiles=" + profiles.keySet() + ", replayStore="
                + (replayStore == null ? "none" : replayStore.getScope().getValue())
                + ", keyMaterial=[REDACTED]}";
    }
}
