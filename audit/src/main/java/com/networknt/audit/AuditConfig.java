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

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class AuditConfig {

    private static final String HEADERS = "headers";
    private static final String AUDIT = "audit";
    private static final String STATUS_CODE = "statusCode";
    private static final String RESPONSE_TIME = "responseTime";
    private static final String AUDIT_ON_ERROR = "auditOnError";
    private static final String IS_LOG_LEVEL_ERROR = "logLevelIsError";
    private static final String IS_MASK_ENABLED = "mask";
    private final Map<String, Object> mappedConfig;
    static final String CONFIG_NAME = "audit";
    private List<String> headerList;
    private List<String> auditList;

    private Config config;
    // A customized logger appender defined in default logback.xml
    private Consumer<String> auditFunc;
    private boolean statusCode;
    private boolean responseTime;
    private boolean auditOnError;
    private boolean isMaskEnabled;

    private AuditConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);

        setLists();
        setLogLevel();
        setConfigData();
    }

    static AuditConfig load() {
        return new AuditConfig();
    }

    List<String> getHeaderList() {
        return headerList;
    }

    List<String> getAuditList() {
        return auditList;
    }

    Consumer<String> getAuditFunc() {
        return auditFunc;
    }

    boolean isAuditOnError() {
        return auditOnError;
    }

    boolean isMaskEnabled() {
        return isMaskEnabled;
    }

    boolean isResponseTime() {
        return responseTime;
    }

    boolean isStatusCode() {
        return statusCode;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    boolean hasHeaderList() {
        return getHeaderList() != null && getHeaderList().size() > 0;
    }

    boolean hasAuditList() {
        return getAuditList() != null && getAuditList().size() > 0;
    }

    Config getConfig() {
        return config;
    }

    private void setLogLevel() {
        Object object = getMappedConfig().get(IS_LOG_LEVEL_ERROR);
        auditFunc = (object != null && (Boolean) object) ?
                LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::error : LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::info;
    }

    private void setLists() {
        headerList = (List<String>) getMappedConfig().get(HEADERS);
        auditList = (List<String>) getMappedConfig().get(AUDIT);
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
        object = getMappedConfig().get(IS_MASK_ENABLED);
        if(object != null && (Boolean) object) {
            isMaskEnabled = true;
        }
    }
}
