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

package com.networknt.client.oauth;

import com.networknt.client.Client;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

/**
 * Created by steve on 02/09/16.
 */
public class ClientCredentialsRequest extends TokenRequest {

    /**
     * load default values from client.json for client credentials grant, overwrite by setters
     * in case you want to change it at runtime.
     */
    public ClientCredentialsRequest() {
        setGrantType(CLIENT_CREDENTIALS);
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Client.CONFIG_NAME);
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                setServerUrl((String)oauthConfig.get(SERVER_URL));
                Map<String, Object> ccConfig = (Map<String, Object>) oauthConfig.get(CLIENT_CREDENTIALS);
                if(ccConfig != null) {
                    setClientId((String)ccConfig.get(CLIENT_ID));
                    setClientSecret((String)ccConfig.get(CLIENT_SECRET));
                    setUri((String)ccConfig.get(URI));
                    setScope((List<String>)ccConfig.get(SCOPE));
                }
            }
        }
    }
}
