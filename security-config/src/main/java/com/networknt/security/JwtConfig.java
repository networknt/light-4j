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
package com.networknt.security;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Jwt Config class
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "jwt", configName = "jwt", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class JwtConfig {
    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);
    public static final String CONFIG_NAME = "jwt";
    public static final String KEY = "key";
    public static final String ISSUER = "issuer";
    public static final String AUDIENCE = "audience";
    public static final String VERSION = "version";
    public static final String EXPIRED_IN_MINUTES = "expiredInMinutes";
    public static final String PROVIDER_ID = "providerId";
    private Map<String, Object> mappedConfig;
    //private Map<String, Object> certificate;
    private final Config config;

    @ObjectField(
            configFieldName = EXPIRED_IN_MINUTES,
            defaultValue = "{\"kid\":\"100\",\"filename\":\"primary.jks\",\"keyName\":\"selfsigned\",\"password\":\"password\"}",
            description = "This is the default JWT configuration and some of the properties need to be overwritten when used to issue JWT tokens.\n" +
                    "It is a component that used to issue JWT token. Normally, it should be used by light-oauth2 or oauth-kafka only.\n" +
                    "Signature private key that used to sign JWT tokens. It is here to ensure backward compatibility only.",
            ref = Key.class
    )
    Key key;

    @StringField(
            configFieldName = ISSUER,
            externalizedKeyName = ISSUER,
            defaultValue = "urn:com:networknt:oauth2:v1",
            externalized = true,
            description = "issuer of the JWT token"
    )
    String issuer;


    @StringField(
            configFieldName = AUDIENCE,
            externalizedKeyName = AUDIENCE,
            defaultValue = "urn:com.networknt",
            externalized = true,
            description = "audience of the JWT token"
    )
    String audience;

    @IntegerField(
            configFieldName = EXPIRED_IN_MINUTES,
            externalizedKeyName = EXPIRED_IN_MINUTES,
            defaultValue = "10",
            externalized = true,
            description = "expired in 10 minutes by default for issued JWT tokens"
    )
    int expiredInMinutes;

    @StringField(
            configFieldName = VERSION,
            externalizedKeyName = VERSION,
            externalized = true,
            defaultValue = "1.0",
            description = "JWT token version"
    )
    String version;


    @StringField(
            configFieldName = PROVIDER_ID,
            externalizedKeyName = PROVIDER_ID,
            externalized = true,
            description = "If federated OAuth 2.0 providers are used, you need to set providerId for each OAuth instance. In most cases, this\n" +
                    "value should be null so that the OAuth 2.0 provider is run as one instance"
    )
    String providerId;

    private JwtConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigMap();
    }
    public static JwtConfig load() {
        return new JwtConfig(CONFIG_NAME);
    }

    public static JwtConfig load(String configName) {
        return new JwtConfig(configName);
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getExpiredInMinutes() {
        return expiredInMinutes;
    }

    public void setExpiredInMinutes(int expiredInMinutes) {
        this.expiredInMinutes = expiredInMinutes;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getProviderId() { return providerId; }

    public void setProviderId(String providerId) { this.providerId = providerId; }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ISSUER);
            if(object != null) issuer = (String)object;
            object = getMappedConfig().get(AUDIENCE);
            if(object != null) audience = (String)object;
            object = getMappedConfig().get(VERSION);
            if(object != null) {
                if(object instanceof Number) {
                    version = object.toString();
                } else {
                    version = (String)object;
                }
            }

            object = getMappedConfig().get(EXPIRED_IN_MINUTES);
            if(object != null) expiredInMinutes = Config.loadIntegerValue(EXPIRED_IN_MINUTES, object);
            object = getMappedConfig().get(PROVIDER_ID);
            if(object != null) providerId = (String)object;
        }
    }

    private void setConfigMap() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(KEY);
            if(object != null) {
                if(object instanceof Map) {
                    key = Config.getInstance().getMapper().convertValue(object, Key.class);
                } else if(object instanceof String) {
                    try {
                        key = Config.getInstance().getMapper().readValue((String)object, Key.class);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else {
                    logger.error("key in jwt.yml is not a map or string");
                }
            }
        }
    }

    public static class Key {

        @StringField(configFieldName = "kid")
        String kid;

        @StringField(configFieldName = "filename")
        String filename;

        @StringField(configFieldName = "password")
        String password;

        @StringField(configFieldName = "keyName")
        String keyName;

        public Key() {
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }
    }
}
