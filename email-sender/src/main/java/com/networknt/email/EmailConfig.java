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

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Email Configuration
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "email",
        configName = "email",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Email Sender Configuration"
)
public class EmailConfig {
    public static final String CONFIG_NAME = "email";

    @StringField(
            configFieldName = "host",
            externalizedKeyName = "host",
            description = "Email server host name or IP address",
            defaultValue = "mail.lightapi.net"
    )
    String host;

    @StringField(
            configFieldName = "port",
            externalizedKeyName = "port",
            defaultValue = "587",
            description = "Email SMTP port number. Please don't use port 25 as it is not safe"
    )
    String port;

    @StringField(
            configFieldName = "user",
            externalizedKeyName = "user",
            description = "Email user or sender address.",
            defaultValue = "noreply@lightapi.net"
    )
    String user;

    @StringField(
            configFieldName = "pass",
            externalizedKeyName = "pass",
            description = "Email password. If you want to put the email.pass in values.yml, you must encrypt it.\n" +
                    "If you don't want to put the password in the config file, you can use the following environment variable.\n" +
                    "EMAIL_PASS=password\n",
            defaultValue = "password"
    )
    String pass;

    @StringField(
            configFieldName = "debug",
            externalizedKeyName = "debug",
            defaultValue = "true",
            description = "Email debug mode. Default to true"
    )
    String debug;

    @StringField(
            configFieldName = "auth",
            externalizedKeyName = "auth",
            defaultValue = "true",
            description = "Email authentication. Default to true"
    )
    String auth;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private static volatile EmailConfig instance;

    private EmailConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }
    private EmailConfig() {
        this(CONFIG_NAME);
    }

    public static EmailConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (EmailConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new EmailConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, EmailConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new EmailConfig(configName);
    }

    public static EmailConfig load() {
        return load(CONFIG_NAME);
    }

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

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(mappedConfig != null) {
            Object object = mappedConfig.get("host");
            if(object != null) host = (String)object;
            object = mappedConfig.get("port");
            if(object != null) port = String.valueOf(object);
            object = mappedConfig.get("user");
            if(object != null) user = (String)object;
            object = mappedConfig.get("pass");
            if(object != null) pass = (String)object;
            object = mappedConfig.get("debug");
            if(object != null) debug = String.valueOf(object);
            object = mappedConfig.get("auth");
            if(object != null) auth = String.valueOf(object);
        }
    }
}
