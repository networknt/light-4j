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
import com.networknt.config.schema.*;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AuditConfig is singleton, and it is loaded from audit.yml in the config folder.
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "audit",
        configName = "audit",
        configDescription = "AuditHandler will pick some important fields from headers and tokens and logs into an audit appender\n" +
                "defined in the logback.xml configuration file.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class AuditConfig {
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

    private Map<String, Object> mappedConfig;
    public static final String CONFIG_NAME = "audit";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable Audit Logging",
            defaultValue = "true"
    )
    private boolean enabled;

    @BooleanField(
            configFieldName = MASK,
            externalizedKeyName = MASK,
            description = "Enable mask in the audit log",
            defaultValue = "true"
    )
    private boolean mask;

    @BooleanField(
            configFieldName = STATUS_CODE,
            externalizedKeyName = STATUS_CODE,
            description = "Output response status code.",
            defaultValue = "true"
    )
    private boolean statusCode;

    @BooleanField(
            configFieldName = RESPONSE_TIME,
            externalizedKeyName = RESPONSE_TIME,
            description = "Output response time.",
            defaultValue = "true"
    )
    private boolean responseTime;

    private final Config config;
    // A customized logger appender defined in default logback.xml
    private Consumer<String> auditFunc;

    @BooleanField(
            configFieldName = AUDIT_ON_ERROR,
            externalizedKeyName = AUDIT_ON_ERROR,
            description = "when auditOnError is true:\n" +
                    " - it will only log when status code >= 400\n" +
                    "when auditOnError is false:\n" +
                    " - it will log on every request\n" +
                    "log level is controlled by logLevel",
            defaultValue = "false"
    )
    private boolean auditOnError;

    @BooleanField(
            configFieldName = LOG_LEVEL_IS_ERROR,
            externalizedKeyName = LOG_LEVEL_IS_ERROR,
            description = "log level is error; by default the logging level is set to info. If you want to change it to error, set to true.",
            defaultValue = "false"
    )
    private boolean logLevelIsError;

    @StringField(
            configFieldName = TIMESTAMP_FORMAT,
            externalizedKeyName = TIMESTAMP_FORMAT,
            description = "the format for outputting the timestamp, if the format is not specified or invalid, will use a long value.\n" +
                    "for some users that will process the audit log manually, you can use yyyy-MM-dd'T'HH:mm:ss.SSSZ as format."
    )
    private String timestampFormat;

    @ArrayField(
            configFieldName = HEADERS,
            externalizedKeyName = HEADERS,
            description = "Output header elements. You can add more if you want. If multiple values, you can use a comma separated\n" +
                    "string as default value in the template and values.yml. You can also use a list of strings in YAML format.\n" +
                    "Correlation Id\n" +
                    "- X-Correlation-Id\n" +
                    "Traceability Id\n" +
                    "- X-Traceability-Id\n" +
                    "caller id for metrics\n" +
                    "- caller_id\n",
            items = String.class,
            defaultValue = "[\"X-Correlation-Id\", \"X-Traceability-Id\",\"caller_id\"]"
    )
    private List<String> headerList;

    @ArrayField(
            configFieldName = AUDIT,
            externalizedKeyName = AUDIT,
            description = "Output audit elements. You can add more if you want. If multiple values, you can use a comma separated\n" +
                    "string as default value in the template and values.yml. You can also use a list of strings in YAML format.\n" +
                    "Client Id\n" +
                    "- client_id\n" +
                    "User Id in id token, this is optional\n" +
                    "- user_id\n" +
                    "Client Id in scope/access token, this is optional\n" +
                    "- scope_client_id\n" +
                    "Request endpoint uri@method.\n" +
                    "- endpoint\n" +
                    "Service ID assigned to the service, this is optional and must be set by the service in its implementation\n" +
                    "- serviceId\n" +
                    "Request Body, this is optional and must be set by the service in its implementation\n" +
                    "- requestBody\n" +
                    "Response payload, this is optional and must be set by the service in its implementation\n" +
                    "- responseBody\n",
            items = String.class,
            defaultValue = "[\"client_id\", \"user_id\", \"scope_client_id\", \"endpoint\", \"serviceId\"]"
    )
    private List<String> auditList;

    @IntegerField(
            configFieldName = REQUEST_BODY_MAX_SIZE,
            externalizedKeyName = REQUEST_BODY_MAX_SIZE,
            description = "The limit of the request body to put into the audit entry if requestBody is in the list of audit. If the\n" +
                    "request body is bigger than the max size, it will be truncated to the max size. The default value is 4096.",
            defaultValue = "4096"
    )
    private int requestBodyMaxSize;

    @IntegerField(
            configFieldName = RESPONSE_BODY_MAX_SIZE,
            externalizedKeyName = RESPONSE_BODY_MAX_SIZE,
            description = "The limit of the response body to put into the audit entry if responseBody is in the list of audit. If the\n" +
                    "response body is bigger than the max size, it will be truncated to the max size. The default value is 4096.",
            defaultValue = "4096"
    )
    private int responseBodyMaxSize;


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

    public boolean isLogLevelIsError() { return logLevelIsError; }

    public boolean isMask() {
        return mask;
    }

    public boolean isEnabled() {
        return enabled;
    }

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
        return getHeaderList() != null && !getHeaderList().isEmpty();
    }

    public boolean hasAuditList() {
        return getAuditList() != null && !getAuditList().isEmpty();
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public int getRequestBodyMaxSize() {
        return requestBodyMaxSize;
    }

    public int getResponseBodyMaxSize() {
        return responseBodyMaxSize;
    }

    Config getConfig() {
        return config;
    }

    private void setLogLevel() {
        auditFunc = auditOnError ? LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::error : LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::info;
    }

    private void setLists() {
        if (getMappedConfig().get(HEADERS) instanceof String) {
            String s = (String) getMappedConfig().get(HEADERS);
            s = s.trim();
            if (logger.isTraceEnabled()) logger.trace("s = " + s);
            if (s.startsWith("[")) {
                // this is a JSON string, and we need to parse it.
                try {
                    headerList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                    });
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
        if (getMappedConfig().get(AUDIT) instanceof String) {
            String s = (String) getMappedConfig().get(AUDIT);
            s = s.trim();
            if (logger.isTraceEnabled()) logger.trace("s = " + s);
            if (s.startsWith("[")) {
                // this is a JSON string, and we need to parse it.
                try {
                    auditList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                    });
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
        if (object != null) statusCode = Config.loadBooleanValue(STATUS_CODE, object);
        object = getMappedConfig().get(RESPONSE_TIME);
        if (object != null) responseTime = Config.loadBooleanValue(RESPONSE_TIME, object);
        object = getMappedConfig().get(AUDIT_ON_ERROR);
        if (object != null) auditOnError = Config.loadBooleanValue(AUDIT_ON_ERROR, object);
        object = getMappedConfig().get(LOG_LEVEL_IS_ERROR);
        if (object != null) logLevelIsError = Config.loadBooleanValue(LOG_LEVEL_IS_ERROR, object);
        object = getMappedConfig().get(MASK);
        if (object != null) mask = Config.loadBooleanValue(MASK, object);
        object = mappedConfig.get(REQUEST_BODY_MAX_SIZE);
        if (object != null) requestBodyMaxSize = Config.loadIntegerValue(REQUEST_BODY_MAX_SIZE, object);
        object = mappedConfig.get(RESPONSE_BODY_MAX_SIZE);
        if (object != null) responseBodyMaxSize = Config.loadIntegerValue(RESPONSE_BODY_MAX_SIZE, object);
        object = getMappedConfig().get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        timestampFormat = (String) getMappedConfig().get(TIMESTAMP_FORMAT);
    }
}
