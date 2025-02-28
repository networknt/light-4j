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

package com.networknt.email;

import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;

/**
 * Email Configuration
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "email", configName = "email", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class EmailConfig {
    public static final String CONFIG_NAME = "email";

    @StringField(
            configFieldName = "host",
            externalizedKeyName = "host",
            externalized = true,
            description = "Email server host name or IP address",
            defaultValue = "mail.lightapi.net"
    )
    String host;

    @StringField(
            configFieldName = "port",
            externalizedKeyName = "port",
            externalized = true,
            defaultValue = "587",
            description = "Email SMTP port number. Please don't use port 25 as it is not safe"
    )
    String port;

    @StringField(
            configFieldName = "user",
            externalizedKeyName = "user",
            externalized = true,
            description = "Email server user name"
    )
    String user;

    @StringField(
            configFieldName = "pass",
            externalizedKeyName = "pass",
            externalized = true,
            description = "Email server password"
    )
    String pass;

    @StringField(
            configFieldName = "debug",
            externalizedKeyName = "debug",
            externalized = true,
            defaultValue = "true",
            description = "Email debug mode"
    )
    String debug;

    @StringField(
            configFieldName = "auth",
            externalizedKeyName = "auth",
            externalized = true,
            defaultValue = "true",
            description = "Email authentication"
    )
    String auth;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(String debug) {
        this.debug = debug;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
}
