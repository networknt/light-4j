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

package com.networknt.encode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.decode.RequestDecodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(configKey = "encode", configName = "response-encode", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class ResponseEncodeConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResponseEncodeConfig.class);

    public static final String CONFIG_NAME = "response-encode";
    public static final String ENABLED = "enabled";
    public static final String ENCODERS = "encoders";
    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "response encode handler for gzip and deflate",
            defaultValue = "false"
    )
    boolean enabled;

    @ArrayField(
            configFieldName = ENCODERS,
            externalizedKeyName = ENCODERS,
            defaultValue = "[\"gzip\", \"deflate\"]",
            items = String.class,
            description = "A list of encoders.\n  -gzip\n  -deflate\n\ngzip,deflate"
    )
    List<String> encoders;

    private ResponseEncodeConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }
    private ResponseEncodeConfig() {
        this(CONFIG_NAME);
    }

    public static ResponseEncodeConfig load(String configName) {
        return new ResponseEncodeConfig(configName);
    }

    public static ResponseEncodeConfig load() {
        return new ResponseEncodeConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getEncoders() {
        return encoders;
    }

    public void setEncoders(List<String> encoders) {
        this.encoders = encoders;
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
        if (mappedConfig != null && mappedConfig.get(ENCODERS) != null) {
            Object object = mappedConfig.get(ENCODERS);
            encoders = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        encoders = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the encoders json with a list of strings.");
                    }
                } else {
                    // comma separated
                    encoders = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    encoders.add((String)item);
                });
            } else {
                throw new ConfigException("encoders must be a string or a list of strings.");
            }
        }
    }

}
