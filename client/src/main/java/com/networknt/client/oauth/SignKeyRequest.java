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
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The configuration is coming from the sign/key section in the client.yml file. This request is used
 * to get the key for sign verification.
 *
 * @author Steve Hu
 */
public class SignKeyRequest extends KeyRequest {
    public static Logger logger = LoggerFactory.getLogger(SignKeyRequest.class);
    public static String SIGN = "sign";

    public SignKeyRequest(String kid) {
        super(kid);
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);

        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> signConfig = (Map<String, Object>)oauthConfig.get(SIGN);
                if(signConfig != null) {
                    Map<String, Object> keyConfig = (Map<String, Object>)signConfig.get(KEY);
                    if(keyConfig != null) {
                        setServerUrl((String)keyConfig.get(SERVER_URL));
                        Object object = keyConfig.get(ENABLE_HTTP2);
                        setEnableHttp2(object != null && (Boolean) object);
                        setUri(keyConfig.get(URI) + "/" + kid);
                        setClientId((String)keyConfig.get(CLIENT_ID));
                        setClientSecret((String)keyConfig.get(CLIENT_SECRET));
                    } else {
                        logger.error("Error: could not find key section in sign of oauth in client.yml");
                    }
                } else {
                    logger.error("Error: could not find sign section of oauth in client.yml");
                }
            } else {
                logger.error("Error: could not find oauth section in client.yml");
            }
        } else {
            logger.error("Error: could not load client.yml for Sign Key");
        }
    }
}
