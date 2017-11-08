/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;

import java.util.Map;

import static java.lang.String.format;

/**
 * For every status response, there is only one message returned. This means the server
 * will fail fast and won't return multiple message at all. Two benefits for this design:
 *
 * 1. low latency as server will return the first error without further processing
 * 2. limited attack risks and make the error handling harder to analyzed
 *
 * @author Steve Hu
 */
public class Status {
    public static final String CONFIG_NAME = "status";
    public static final Map<String, Object> config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    // status serialization bean
    // allows API implementations to provide their own Status serialization mechanism
    static StatusSerializer statusSerializer;

    int statusCode;
    String code;
    String message;
    String description;

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
     *
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
        Map<String, Object> map = (Map<String, Object>)config.get(code);
        if(map != null) {
            this.statusCode = (Integer)map.get("statusCode");
            this.message = (String)map.get("message");
            this.description = format((String)map.get("description"), args);
        }
    }

    /**
     * Construct a status object based on all the properties in the object. It is not
     * very often to use this construct to create object.
     *
     * @param statusCode Status Code
     * @param code Code
     * @param message Message
     * @param description Description
     */
    public Status(int statusCode, String code, String message, String description) {
        this.statusCode = statusCode;
        this.code = code;
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

    @Override
    public String toString() {
    	if(statusSerializer != null) {
    	    return statusSerializer.serializeStatus(this);
        } else {
            return "{\"statusCode\":" + getStatusCode()
                    + ",\"code\":\"" + getCode()
                    + "\",\"message\":\""
                    + getMessage() + "\",\"description\":\""
                    + getDescription() + "\"}";
        }
    }
}
