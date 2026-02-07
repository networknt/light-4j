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

package com.networknt.token.exchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for Token Exchange Handler.
 * Reads from token-exchange.yml
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "token-exchange",
        configName = "token-exchange",
        configDescription = "Token Exchange Handler configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class TokenExchangeConfig {
    public static final String CONFIG_NAME = "token-exchange";
    public static final String ENABLED = "enabled";
    public static final String TOKEN_EX_URI = "tokenExUri";
    public static final String TOKEN_EX_CLIENT_ID = "tokenExClientId";
    public static final String TOKEN_EX_CLIENT_SECRET = "tokenExClientSecret";
    public static final String TOKEN_EX_SCOPE = "tokenExScope";
    public static final String SUBJECT_TOKEN_TYPE = "subjectTokenType";
    public static final String REQUESTED_TOKEN_TYPE = "requestedTokenType";
    public static final String MAPPING_STRATEGY = "mappingStrategy";
    public static final String CLIENT_MAPPING = "clientMapping";

    private final Map<String, Object> mappedConfig;
    private static TokenExchangeConfig instance;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable Token Exchange Handler",
            defaultValue = "true"
    )
    private boolean enabled;

    @StringField(
            configFieldName = TOKEN_EX_URI,
            externalizedKeyName = TOKEN_EX_URI,
            description = "The path to the token exchange endpoint on OAuth 2.0 provider"
    )
    private String tokenExUri;

    @StringField(
            configFieldName = TOKEN_EX_CLIENT_ID,
            externalizedKeyName = TOKEN_EX_CLIENT_ID,
            description = "The client ID for the token exchange"
    )
    private String tokenExClientId;

    @StringField(
            configFieldName = TOKEN_EX_CLIENT_SECRET,
            externalizedKeyName = TOKEN_EX_CLIENT_SECRET,
            description = "The client secret for the token exchange"
    )
    private String tokenExClientSecret;

    @ArrayField(
            configFieldName = TOKEN_EX_SCOPE,
            externalizedKeyName = TOKEN_EX_SCOPE,
            description = "The scope of the returned token",
            items = String.class
    )
    private List<String> tokenExScope;

    @StringField(
            configFieldName = SUBJECT_TOKEN_TYPE,
            externalizedKeyName = SUBJECT_TOKEN_TYPE,
            description = "The subject token type",
            defaultValue = "urn:ietf:params:oauth:token-type:jwt"
    )
    private String subjectTokenType;

    @StringField(
            configFieldName = REQUESTED_TOKEN_TYPE,
            externalizedKeyName = REQUESTED_TOKEN_TYPE,
            description = "The requested token type",
            defaultValue = "urn:ietf:params:oauth:token-type:jwt"
    )
    private String requestedTokenType;

    @StringField(
            configFieldName = MAPPING_STRATEGY,
            externalizedKeyName = MAPPING_STRATEGY,
            description = "Mapping of external client IDs to internal function IDs (database or config)",
            defaultValue = "database"
    )
    private String mappingStrategy;

    @MapField(
            configFieldName = CLIENT_MAPPING,
            externalizedKeyName = CLIENT_MAPPING,
            description = "If mappingStrategy is 'config', define the map here",
            valueType = String.class
    )
    private Map<String, String> clientMapping;

    private TokenExchangeConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private TokenExchangeConfig() {
        this(CONFIG_NAME);
    }

    public static TokenExchangeConfig load() {
        return load(CONFIG_NAME);
    }

    public static TokenExchangeConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (TokenExchangeConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new TokenExchangeConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, TokenExchangeConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new TokenExchangeConfig(configName);
    }

    public void reload() {
        mappedConfig.clear();
        mappedConfig.putAll(Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME));
        setConfigData();
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            tokenExUri = (String)mappedConfig.get(TOKEN_EX_URI);
            tokenExClientId = (String)mappedConfig.get(TOKEN_EX_CLIENT_ID);
            tokenExClientSecret = (String)mappedConfig.get(TOKEN_EX_CLIENT_SECRET);
            if(mappedConfig.get(TOKEN_EX_SCOPE) != null) {
                tokenExScope = (List<String>)mappedConfig.get(TOKEN_EX_SCOPE);
            }
            subjectTokenType = (String)mappedConfig.get(SUBJECT_TOKEN_TYPE);
            requestedTokenType = (String)mappedConfig.get(REQUESTED_TOKEN_TYPE);
            mappingStrategy = (String)mappedConfig.get(MAPPING_STRATEGY);
            if(mappedConfig.get(CLIENT_MAPPING) != null) {
                clientMapping = (Map<String, String>)mappedConfig.get(CLIENT_MAPPING);
            }
        }
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTokenExUri() {
        return tokenExUri;
    }

    public String getTokenExClientId() {
        return tokenExClientId;
    }

    public String getTokenExClientSecret() {
        return tokenExClientSecret;
    }

    public List<String> getTokenExScope() {
        return tokenExScope;
    }

    public String getSubjectTokenType() {
        return subjectTokenType;
    }

    public String getRequestedTokenType() {
        return requestedTokenType;
    }

    public String getMappingStrategy() {
        return mappingStrategy;
    }

    public Map<String, String> getClientMapping() {
        return clientMapping;
    }
}
