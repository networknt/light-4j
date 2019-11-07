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
import com.networknt.client.Http2Client;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
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

    /**
     * @deprecated will be moved to {@link ClientConfig#TOKEN}
     */
    @Deprecated
    public static String TOKEN = "token";

    public TokenKeyRequest(String kid) {
        super(kid);
        Map<String, Object> clientConfig = ClientConfig.get().getMappedConfig();
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(ClientConfig.OAUTH);
            if(oauthConfig != null) {
                // for backward compatible here, should be moved to the token section.
                Map<String, Object> keyConfig = (Map<String, Object>)oauthConfig.get(ClientConfig.KEY);
                if(keyConfig != null) {
                    setKeyOptions(keyConfig);
                    Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);
                    setClientSecret((String)secret.get(SecretConstants.KEY_CLIENT_SECRET));
                } else {
                    // there is no key section under oauth. look up in the oauth/token section for key
                    Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
                    if(tokenConfig != null) {
                        keyConfig = (Map<String, Object>)tokenConfig.get(ClientConfig.KEY);
                        if(keyConfig != null) {
                            setKeyOptions(keyConfig);
                            setClientSecret((String)keyConfig.get(ClientConfig.CLIENT_SECRET));
                        } else {
                            logger.error("Error: could not find key section in token of oauth in client.yml");
                        }
                    } else {
                        logger.error("Error: could not find token section of oauth in client.yml");
                    }
                }
            } else {
                logger.error("Error: could not find oauth section in client.yml");
            }
        } else {
            logger.error("Error: could not load client.yml for Token Key");
        }
    }

    private void setKeyOptions(Map<String, Object> keyConfig) {
        setServerUrl((String)keyConfig.get(ClientConfig.SERVER_URL));
        setServiceId((String)keyConfig.get(ClientConfig.SERVICE_ID));
        Object object = keyConfig.get(ClientConfig.ENABLE_HTTP2);
        setEnableHttp2(object != null && (Boolean) object);
        setUri(keyConfig.get(ClientConfig.URI) + "/" + kid);
        setClientId((String)keyConfig.get(ClientConfig.CLIENT_ID));
    }

}

