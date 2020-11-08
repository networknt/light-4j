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

package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The request that is used to get the key for token verification. It first check the token/key section in the
 * client.yml and then key section of token for backward compatibility. It is recommended to set the key under
 * token to clear indicate that the key is for token verification.
 *
 * @author Steve Hu
 */
public class TokenKeyRequest extends KeyRequest {
    private static Logger logger = LoggerFactory.getLogger(TokenKeyRequest.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";

    private boolean jwk;

    /**
     * @deprecated will be moved to {@link ClientConfig#TOKEN}
     */
    @Deprecated
    public static String TOKEN = "token";
    public TokenKeyRequest(String kid) {
        this(kid, false);
    }
    public TokenKeyRequest(String kid, boolean jwk) {
        super(kid);
        this.jwk = jwk;
        Map<String, Object> clientConfig = ClientConfig.get().getMappedConfig();
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(ClientConfig.OAUTH);
            if(oauthConfig != null) {
                // for backward compatible here, should be moved to the token section.
                Map<String, Object> keyConfig = (Map<String, Object>)oauthConfig.get(ClientConfig.KEY);
                if(keyConfig != null) {
                    setKeyOptions(keyConfig);
                } else {
                    // there is no key section under oauth. look up in the oauth/token section for key
                    Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
                    if(tokenConfig != null) {
                        keyConfig = (Map<String, Object>)tokenConfig.get(ClientConfig.KEY);
                        if(keyConfig != null) {
                            setKeyOptions(keyConfig);
                        } else {
                            logger.error(new Status(CONFIG_PROPERTY_MISSING, "token section", "client.yml").toString());
                        }
                    } else {
                        logger.error(new Status(CONFIG_PROPERTY_MISSING, "token section", "client.yml").toString());
                    }
                }
            } else {
                logger.error(new Status(CONFIG_PROPERTY_MISSING, "oauth section", "client.yml").toString());
            }
        } else {
            logger.error(new Status(CONFIG_PROPERTY_MISSING, "oauth key section", "client.yml").toString());
        }
    }

    private void setKeyOptions(Map<String, Object> keyConfig) {
        setServerUrl((String)keyConfig.get(ClientConfig.SERVER_URL));
        setProxyHost((String)keyConfig.get(ClientConfig.PROXY_HOST));
        int port = keyConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)keyConfig.get(ClientConfig.PROXY_PORT);
        setProxyPort(port);
        setServiceId((String)keyConfig.get(ClientConfig.SERVICE_ID));
        Object object = keyConfig.get(ClientConfig.ENABLE_HTTP2);
        setEnableHttp2(object != null && (Boolean) object);
        if(jwk) {
            // there is no additional kid in the path parameter for jwk
            setUri(keyConfig.get(ClientConfig.URI).toString());
        } else {
            setUri(keyConfig.get(ClientConfig.URI) + "/" + kid);
        }
        setClientId((String)keyConfig.get(ClientConfig.CLIENT_ID));
        if(keyConfig.get(ClientConfig.CLIENT_SECRET) != null) {
            setClientSecret((String)keyConfig.get(ClientConfig.CLIENT_SECRET));
        } else {
            logger.error(new Status(CONFIG_PROPERTY_MISSING, "refresh_token client_secret", "client.yml").toString());
       }
    }
}

