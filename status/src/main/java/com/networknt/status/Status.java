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

package com.networknt.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IllegalFormatException;
import java.util.Map;

import static java.lang.String.format;

/**
 * For every status response, there is only one message returned. This means the server
 * will fail fast and won't return multiple message at all. Two benefits for this design:
 * <p>
 * 1. low latency as server will return the first error without further processing
 * 2. limited attack risks and make the error handling harder to analyzed
 *
 * @author Steve Hu
 */
public class Status {
    private static final Logger logger = LoggerFactory.getLogger(Status.class);

    public static final String CONFIG_NAME = "status";
    public static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    // default severity
    public static final String defaultSeverity = "ERROR";
    public static final String SHOW_METADATA = "showMetadata";
    public static final String SHOW_DESCRIPTION = "showDescription";
    public static final String SHOW_MESSAGE = "showMessage";

    // status serialization bean
    // allows API implementations to provide their own Status serialization mechanism
    private static StatusSerializer statusSerializer;

    private int statusCode;
    private String code;
    private String severity;
    private String message;
    private String description;
    private Map<String, Object> metadata;
    private boolean showMetadata;
    private boolean showDescription;
    private boolean showMessage;

    static {
        ModuleRegistry.registerModule(Status.class.getName(), config, null);
        try {
            statusSerializer = SingletonServiceFactory.getBean(StatusSerializer.class);
        } catch (ExceptionInInitializerError e) {
            statusSerializer = null;
        }
    }

    /**
     * Default construction that is only used in reflection.
     */
    public Status() {
    }

    /**
     * Construct a status object based on error code and a list of arguments. It is
     * the most popular way to create status object from status.yml definition.
     *
     * @param code Error Code
     * @param args A list of arguments that will be populated into the error description
     */
    public Status(final String code, final Object... args) {
        this.code = code;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) config.get(code);
        if (map != null) {
            this.statusCode = (Integer) map.get("statusCode");
            this.message = (String) map.get("message");
            this.description = (String) map.get("description");
            try {
                this.description = format(this.description, args);
            } catch (IllegalFormatException e) {
//                logger.warn(format("Error formatting description of status %s", code), e);
            }
            if ((this.severity = (String) map.get("severity")) == null)
                this.severity = defaultSeverity;

        }
    }

    /**
     * Construct a status object based on error code and a list of arguments. It is
     * the most popular way to create status object from status.yml definition.
     *
     * @param code Error Code
     * @param args A list of arguments that will be populated into the error description
     */
    public Status(final String code, final Map<String, Object> metadata, final Object... args) {
        this.code = code;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) config.get(code);
        if (map != null) {
            this.statusCode = (Integer) map.get("statusCode");
            this.message = (String) map.get("message");
            this.description = (String) map.get("description");
            this.metadata = metadata;
            try {
                this.description = format(this.description, args);
            } catch (IllegalFormatException e) {
//                logger.warn(format("Error formatting description of status %s", code), e);
            }
            if ((this.severity = (String) map.get("severity")) == null)
                this.severity = defaultSeverity;

        }
    }

    /**
     * Construct a status object based on all the properties in the object. It is not
     * very often to use this construct to create object.
     *
     * @param statusCode  Status Code
     * @param code        Code
     * @param message     Message
     * @param description Description
     */
    public Status(int statusCode, String code, String message, String description) {
        this.statusCode = statusCode;
        this.code = code;
        this.severity = defaultSeverity;
        this.message = message;
        this.description = description;
    }

    /**
     * Construct a status object based on all the properties in the object. It is not
     * very often to use this construct to create object.
     *
     * @param httpStatus  HttpStatus
     * @param message     Message
     * @param description Description
     */
    public Status(HttpStatus httpStatus, String message, String description) {
        this.statusCode = httpStatus.value();
        this.code = httpStatus.getReasonPhrase();
        this.severity = defaultSeverity;
        this.message = message;
        this.description = description;
    }


    /**
     * Construct a status object based on all the properties in the object. It is not
     * very often to use this construct to create object.
     *
     * @param statusCode  Status Code
     * @param code        Code
     * @param severity    Status Severity
     * @param message     Message
     * @param description Description
     */
    public Status(int statusCode, String code, String message, String description, String severity) {
        this.statusCode = statusCode;
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.description = description;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * put key value pair to the metadata
     * @param key key of the entry
     * @param value value of the entry
     */
    public void putMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        showMessage = config.get(SHOW_MESSAGE) == null ? true : (boolean)config.get(SHOW_MESSAGE);
        showMetadata = config.get(SHOW_METADATA) == null ? false : (boolean)config.get(SHOW_METADATA);
        showDescription = config.get(SHOW_DESCRIPTION) == null ? true : (boolean)config.get(SHOW_DESCRIPTION);
        if (statusSerializer != null) {
            return statusSerializer.serializeStatus(this);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
            .append("\"statusCode\":" + getStatusCode())
            .append(",\"code\":\"" + getCode());
            if (showMessage) {
                sb.append("\",\"message\":\"" + getMessage());
            }
            if (showDescription) {
                sb.append("\",\"description\":\"" + getDescription());
            }
            if (showMetadata) {
                try {
                    sb.append("\",\"metadata\":\"" + Config.getInstance().getMapper().writeValueAsString(getMetadata()));
                } catch (JsonProcessingException e) {
                    logger.error("cannot parse metadata for status:" + getStatusCode(), e);
                }
            }
            sb.append("\",\"severity\":\"" + getSeverity());
            sb.append("\"}");

            return sb.toString();
        }
    }
}
