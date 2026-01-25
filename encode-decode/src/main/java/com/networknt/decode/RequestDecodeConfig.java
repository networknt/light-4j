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

package com.networknt.decode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(configKey = "decode", configName = "request-decode", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class RequestDecodeConfig {
    private static final Logger logger = LoggerFactory.getLogger(RequestDecodeConfig.class);
    public static final String CONFIG_NAME = "request-decode";
    public static final String ENABLED = "enabled";
    public static final String DECODERS = "decoders";
    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "request decode handler for gzip and deflate",
            defaultValue = "false"
    )
    boolean enabled;

    @ArrayField(
            configFieldName = DECODERS,
            externalizedKeyName = DECODERS,
            defaultValue = "[\"gzip\", \"deflate\"]",
            items = String.class,
            description = "A list of decoders.\n  -gzip\n  -deflate\ngzip,deflate"
    )
    List<String> decoders;

    private static volatile RequestDecodeConfig instance;

    private RequestDecodeConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
        setConfigList();
    }
    private RequestDecodeConfig() {
        this(CONFIG_NAME);
    }

    public static RequestDecodeConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (RequestDecodeConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new RequestDecodeConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, RequestDecodeConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new RequestDecodeConfig(configName);
    }

    public static RequestDecodeConfig load() {
        return load(CONFIG_NAME);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDecoders() {
        return decoders;
    }

    public void setDecoders(List<String> decoders) {
        this.decoders = decoders;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if (getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        }
    }

    private void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(DECODERS) != null) {
            Object object = mappedConfig.get(DECODERS);
            decoders = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        decoders = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the decoders json with a list of strings.");
                    }
                } else {
                    // comma separated
                    decoders = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    decoders.add((String)item);
                });
            } else {
                throw new ConfigException("decoders must be a string or a list of strings.");
            }
        }
    }

}
