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
 * Created by steve on 23/09/16.
 */
public class Status {
    public static final String CONFIG_NAME = "status";
    public static final Map<String, Object> config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    int statusCode;
    String code;
    String message;
    String description;

    static {
        ModuleRegistry.registerModule(Status.class.getName(), config, null);
    }

    public Status() {
    }

    public Status(final String code, final Object... args) {
        this.code = code;
        Map<String, Object> map = (Map<String, Object>)config.get(code);
        if(map != null) {
            this.statusCode = (Integer)map.get("statusCode");
            this.message = (String)map.get("message");
            this.description = format((String)map.get("description"), args);
        }
    }

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
        return "{\"statusCode\":" + statusCode
                + ",\"code\":\"" + code
                + "\",\"message\":\""
                + message + "\",\"description\":\""
                + description + "\"}";
    }

    /*
    public String toStringAppend() {
        StringBuilder builder = new StringBuilder("{\"statusCode\":");
        return builder.append(statusCode)
                .append(",\"code\":\"")
                .append(code)
                .append("\",\"message\":\"")
                .append(message)
                .append("\",\"description\":\"")
                .append(description)
                .append("\"}").toString();
    }
    */

}
