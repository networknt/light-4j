/*
 * Copyright (c) 2019 Network New Technologies Inc.
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
package com.networknt.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class AuditConfig {
    private static final Logger logger = LoggerFactory.getLogger(AuditConfig.class);

    public static final String REQUEST_BODY = "requestBody";
    public static final String RESPONSE_BODY = "responseBody";

    private static final String HEADERS = "headers";
    private static final String AUDIT = "audit";
    private static final String STATUS_CODE = "statusCode";
    private static final String RESPONSE_TIME = "responseTime";
    private static final String AUDIT_ON_ERROR = "auditOnError";
    private static final String LOG_LEVEL_IS_ERROR = "logLevelIsError";
    private static final String MASK = "mask";
    private static final String TIMESTAMP_FORMAT = "timestampFormat";
    private static final String ENABLED = "enabled";
    private static final String REQUEST_BODY_MAX_SIZE = "requestBodyMaxSize";
    private static final String RESPONSE_BODY_MAX_SIZE = "responseBodyMaxSize";

    private  Map<String, Object> mappedConfig;
    public static final String CONFIG_NAME = "audit";
    private List<String> headerList;
    private List<String> auditList;

    private Config config;
    // A customized logger appender defined in default logback.xml
    private Consumer<String> auditFunc;
    private boolean statusCode;
    private boolean responseTime;
    private boolean auditOnError;
    private boolean mask;
    private String timestampFormat;
    private int requestBodyMaxSize;
    private int responseBodyMaxSize;
    private boolean enabled;

    private AuditConfig() {
        this(CONFIG_NAME);
    }

    private AuditConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);

        setLists();
        setLogLevel();
        setConfigData();
    }

    public static AuditConfig load() {
        return new AuditConfig();
    }

    public static AuditConfig load(String configName) {
        return new AuditConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);

        setLists();
        setLogLevel();
        setConfigData();
    }

    public List<String> getHeaderList() {
        return headerList;
    }

    public List<String> getAuditList() {
        return auditList;
    }

    public Consumer<String> getAuditFunc() {
        return auditFunc;
    }

    public boolean isAuditOnError() {
        return auditOnError;
    }

    public boolean isMask() {
        return mask;
    }

    public boolean isEnabled() { return enabled; }

    public boolean isResponseTime() {
        return responseTime;
    }

    public boolean isStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean hasHeaderList() {
        return getHeaderList() != null && getHeaderList().size() > 0;
    }

    public boolean hasAuditList() {
        return getAuditList() != null && getAuditList().size() > 0;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public int getRequestBodyMaxSize() { return requestBodyMaxSize; }

    public int getResponseBodyMaxSize() { return responseBodyMaxSize; }

    Config getConfig() {
        return config;
    }

    private void setLogLevel() {
        Object object = getMappedConfig().get(LOG_LEVEL_IS_ERROR);
        auditFunc = (object != null && (Boolean) object) ?
                LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::error : LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::info;
    }

    private void setLists() {
        if(getMappedConfig().get(HEADERS) instanceof String) {
            String s = (String)getMappedConfig().get(HEADERS);
            s = s.trim();
            if(logger.isTraceEnabled()) logger.trace("s = " + s);
            if(s.startsWith("[")) {
                // this is a JSON string, and we need to parse it.
                try {
                    headerList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    throw new ConfigException("could not parse the headers json with a list of strings.");
                }
            } else {
                // this is a comma separated string.
                headerList = Arrays.asList(s.split("\\s*,\\s*"));
            }
        } else if (getMappedConfig().get(HEADERS) instanceof List) {
            headerList = (List<String>) getMappedConfig().get(HEADERS);
        } else {
            throw new ConfigException("headers list is missing or wrong type.");
        }
        if(getMappedConfig().get(AUDIT) instanceof String) {
            String s = (String)getMappedConfig().get(AUDIT);
            s = s.trim();
            if(logger.isTraceEnabled()) logger.trace("s = " + s);
            if(s.startsWith("[")) {
                // this is a JSON string, and we need to parse it.
                try {
                    auditList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    throw new ConfigException("could not parse the audit json with a list of strings.");
                }
            } else {
                // this is a comma separated string.
                auditList = Arrays.asList(s.split("\\s*,\\s*"));
            }
        } else if (getMappedConfig().get(AUDIT) instanceof List) {
            auditList = (List<String>) getMappedConfig().get(AUDIT);
        } else {
            throw new ConfigException("audit list is missing or wrong type.");
        }
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(STATUS_CODE);
        if(object != null && (Boolean) object) {
            statusCode = true;
        }
        object = getMappedConfig().get(RESPONSE_TIME);
        if(object != null && (Boolean) object) {
            responseTime = true;
        }
        // audit on error response flag
        object = getMappedConfig().get(AUDIT_ON_ERROR);
        if(object != null && (Boolean) object) {
            auditOnError = true;
        }
        object = getMappedConfig().get(MASK);
        if(object != null && (Boolean) object) {
            mask = true;
        }
        object = mappedConfig.get(REQUEST_BODY_MAX_SIZE);
        if (object != null) {
            requestBodyMaxSize = (Integer) object;
        }
        object = mappedConfig.get(RESPONSE_BODY_MAX_SIZE);
        if (object != null) {
            responseBodyMaxSize = (Integer) object;
        }
        object = getMappedConfig().get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
        timestampFormat = (String)getMappedConfig().get(TIMESTAMP_FORMAT);
    }
}
