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

import com.networknt.client.Http2Client;
import com.networknt.client.oauth.constant.OauthConfigConstants;
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
    public static Logger logger = LoggerFactory.getLogger(TokenKeyRequest.class);
    public static String TOKEN = "token";

    public TokenKeyRequest(String kid) {
        super(kid);
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                // for backward compatible here, should be moved to the token section.
                Map<String, Object> keyConfig = (Map<String, Object>)oauthConfig.get(KEY);
                if(keyConfig != null) {
                    setServerUrl((String)keyConfig.get(OauthConfigConstants.SERVER_URL));
                    setServiceId((String)keyConfig.get(OauthConfigConstants.SERVICE_ID));
                    Object object = keyConfig.get(OauthConfigConstants.ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    setUri(keyConfig.get(OauthConfigConstants.URI) + "/" + kid);
                    setClientId((String)keyConfig.get(OauthConfigConstants.CLIENT_ID));
                    Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);
                    setClientSecret((String)secret.get(SecretConstants.KEY_CLIENT_SECRET));
                } else {
                    // there is no key section under oauth. look up in the oauth/token section for key
                    Map<String, Object> tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
                    if(tokenConfig != null) {
                        keyConfig = (Map<String, Object>)tokenConfig.get(OauthConfigConstants.KEY);
                        if(keyConfig != null) {
                            setServerUrl((String)keyConfig.get(OauthConfigConstants.SERVER_URL));
                            setServiceId((String)keyConfig.get(OauthConfigConstants.SERVICE_ID));
                            Object object = keyConfig.get(OauthConfigConstants.ENABLE_HTTP2);
                            setEnableHttp2(object != null && (Boolean) object);
                            setUri(keyConfig.get(OauthConfigConstants.URI) + "/" + kid);
                            setClientId((String)keyConfig.get(OauthConfigConstants.CLIENT_ID));
                            setClientSecret((String)keyConfig.get(OauthConfigConstants.CLIENT_SECRET));
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

}

