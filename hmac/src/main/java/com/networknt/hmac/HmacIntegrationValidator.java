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
import com.networknt.handler.RequestInjectionConfig;
import com.networknt.handler.RequestInterceptor;
import com.networknt.reqtrans.RequestTransformerConfig;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.server.ServerConfig;

import java.util.List;

/** Startup validation for the exact request-buffering boundary required by HMAC routes. */
public final class HmacIntegrationValidator {
    private HmacIntegrationValidator() {
    }

    public static void validate(HmacRuntime runtime,
                                UnifiedSecurityConfig unifiedConfig,
                                RequestInjectionConfig requestInjection,
                                ServerConfig serverConfig) {
        validate(runtime, unifiedConfig, requestInjection, serverConfig, RequestTransformerConfig.load());
    }

    public static void validate(HmacRuntime runtime,
                                UnifiedSecurityConfig unifiedConfig,
                                RequestInjectionConfig requestInjection,
                                ServerConfig serverConfig,
                                RequestTransformerConfig requestTransformer) {
        List<UnifiedPathPrefixAuth> rules = unifiedConfig.getPathPrefixAuths();
        if (rules == null || rules.stream().noneMatch(rule -> rule.getHmacProfile() != null))
            return;
        if (!requestInjection.isEnabled())
            throw new ConfigException("request-injection must be enabled for HMAC routes.");
        if (requestInjection.getMaxBodyBytes() <= 0)
            throw new ConfigException("request-injection.maxBodyBytes must be positive for HMAC routes.");
        if (requestInjection.getMaxBuffers() <= 0 || serverConfig.getBufferSize() <= 0)
            throw new ConfigException("request-injection.maxBuffers and server.bufferSize must be positive for HMAC routes.");

        long retainedCapacity = (long) requestInjection.getMaxBuffers() * serverConfig.getBufferSize();
        if (retainedCapacity < requestInjection.getMaxBodyBytes())
            throw new ConfigException("request-injection.maxBuffers cannot retain request-injection.maxBodyBytes with the configured server.bufferSize.");

        for (UnifiedPathPrefixAuth rule : rules) {
            if (rule.getHmacProfile() == null)
                continue;
            HmacProfileRuntime profile = runtime.getProfile(rule.getHmacProfile());
            if (profile == null)
                throw new ConfigException("HMAC profile " + rule.getHmacProfile() + " is unavailable.");
            if (requestInjection.getMaxBodyBytes() < profile.getMaxBodyBytes())
                throw new ConfigException("request-injection.maxBodyBytes is smaller than HMAC profile "
                        + profile.getName() + ".maxBodyBytes.");
            if (requestInjection.getAppliedBodyInjectionPathPrefixes() == null
                    || requestInjection.getAppliedBodyInjectionPathPrefixes().stream()
                    .noneMatch(rule.getPrefix()::startsWith))
                throw new ConfigException("HMAC prefix " + rule.getPrefix()
                        + " is not covered by request-injection.appliedBodyInjectionPathPrefixes.");
            if (requestTransformer != null && requestTransformer.isEnabled()
                    && requestTransformer.getAppliedPathPrefixes() != null) {
                for (String transformerPrefix : requestTransformer.getAppliedPathPrefixes()) {
                    if (transformerPrefix != null && (rule.getPrefix().startsWith(transformerPrefix)
                            || transformerPrefix.startsWith(rule.getPrefix())))
                        throw new ConfigException("HMAC prefix " + rule.getPrefix()
                                + " overlaps request-transformer prefix " + transformerPrefix + '.');
                }
            }
        }
    }

    public static void validateInterceptorOrder(RequestInterceptor[] interceptors) {
        if (interceptors == null || interceptors.length == 0
                || !(interceptors[0] instanceof HmacRequestInterceptor))
            throw new ConfigException("HmacRequestInterceptor must be the first RequestInterceptor in service.yml.");
    }

}
