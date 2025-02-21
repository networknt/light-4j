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
package com.networknt.correlation;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by steve on 29/09/16.
 */
@ConfigSchema(configKey = "correlation", configName = "correlation", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class CorrelationConfig {
    public static final String CONFIG_NAME = "correlation";
    private static final String ENABLED = "enabled";
    private static final String AUTOGEN_CORRELATION_ID = "autogenCorrelationID";
    private static final String TRACEABILITY_MDC_FIELD = "traceabilityMdcField";
    private static final String CORRELATION_MDC_FIELD = "correlationMdcField";
    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            defaultValue = true,
            externalized = true,
            description = "If enabled is true, the handler will be injected into the request and response chain."
    )
    boolean enabled;

    @BooleanField(
            configFieldName = AUTOGEN_CORRELATION_ID,
            defaultValue = true,
            externalized = true,
            description = "If set to true, it will auto-generate the correlationID if it is not provided in the request"
    )
    boolean autogenCorrelationID;

    @StringField(
            configFieldName = CORRELATION_MDC_FIELD,
            externalized = true,
            defaultValue = "cId",
            description = "The MDC context field name for the correlation id value"
    )
    String correlationMdcField;

    @StringField(
            configFieldName = TRACEABILITY_MDC_FIELD,
            externalized = true,
            defaultValue = "tId",
            description = "The MDC context field name for the traceability id value"
    )
    String traceabilityMdcField;

    private CorrelationConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private CorrelationConfig() {
        this(CONFIG_NAME);
    }

    public static CorrelationConfig load(String configName) {
        return new CorrelationConfig(configName);
    }

    public static CorrelationConfig load() {
        return new CorrelationConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTraceabilityMdcField() {
        return traceabilityMdcField;
    }

    public void setTraceabilityMdcField(String traceabilityMdcField) {
        this.traceabilityMdcField = traceabilityMdcField;
    }

    public String getCorrelationMdcField() {
        return correlationMdcField;
    }

    public void setCorrelationMdcField(String correlationMdcField) {
        this.correlationMdcField = correlationMdcField;
    }

    public boolean isAutogenCorrelationID() {
    	return autogenCorrelationID;
    }

    public void setAutogenCorrelationID(boolean autogenCorrelationID) {
    	this.autogenCorrelationID = autogenCorrelationID;
    }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = getMappedConfig().get(AUTOGEN_CORRELATION_ID);
            if(object != null) autogenCorrelationID = Config.loadBooleanValue(AUTOGEN_CORRELATION_ID, object);
            object = getMappedConfig().get(TRACEABILITY_MDC_FIELD);
            if(object != null) traceabilityMdcField = (String)object;
            object = getMappedConfig().get(CORRELATION_MDC_FIELD);
            if(object != null) correlationMdcField = (String)object;
        }
    }
}
