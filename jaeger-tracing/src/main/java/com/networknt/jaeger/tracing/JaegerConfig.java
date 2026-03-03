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

package com.networknt.jaeger.tracing;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.Map;

public class JaegerConfig {
    public final static String CONFIG_NAME = "jaeger-tracing";
    public final static String ENABLED = "enabled";
    public final static String TYPE = "type";
    public final static String PARAM = "param";

    boolean enabled;
    String type;
    Number param;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private JaegerConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private JaegerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static JaegerConfig load() {
        return new JaegerConfig();
    }

    public static JaegerConfig load(String configName) {
        return new JaegerConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Number getParam() {
        return param;
    }

    public void setParam(Number param) {
        this.param = param;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(TYPE);
        if (object != null) type = (String)object;
        object = mappedConfig.get(PARAM);
        if(object != null) {
            if (object instanceof Number) {
                param = (Number) object;
            } else if (object instanceof String) {
                // convert to integer if there is no period in the string. Otherwise, it is a double.
                if(((String) object).indexOf('.') == -1) {
                    param = Integer.valueOf((String) object);
                } else {
                    param = Double.valueOf((String) object);
                }
            } else {
                throw new ConfigException(PARAM + " must be an integer or a double or a string value.");
            }
        }
    }
}
