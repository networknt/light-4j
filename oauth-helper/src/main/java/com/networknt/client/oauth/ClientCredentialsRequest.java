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

import java.util.List;
import java.util.Map;

/**
 * load default values from client.yml for client credentials grant, overwrite by setters
 * in case you want to change it at runtime.
 *
 * @author Steve Hu
 */
public class ClientCredentialsRequest extends TokenRequest {
    private static final Logger logger = LoggerFactory.getLogger(ClientCredentialsRequest.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";
    public ClientCredentialsRequest() {
        this(null);
    }

    public ClientCredentialsRequest(Map<String, Object> ccConfig) {
        setGrantType(ClientConfig.CLIENT_CREDENTIALS);
        Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
        if(tokenConfig != null) {
            setServerUrl((String)tokenConfig.get(ClientConfig.SERVER_URL));
            setProxyHost((String)tokenConfig.get(ClientConfig.PROXY_HOST));
            int port = tokenConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)tokenConfig.get(ClientConfig.PROXY_PORT);
            setProxyPort(port);
            setServiceId((String)tokenConfig.get(ClientConfig.SERVICE_ID));
            Object object = tokenConfig.get(ClientConfig.ENABLE_HTTP2);
            setEnableHttp2(object != null && (Boolean) object);
            if(ccConfig == null) ccConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.CLIENT_CREDENTIALS);
            if(ccConfig != null) {
                setClientId((String)ccConfig.get(ClientConfig.CLIENT_ID));
                if(ccConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                    setClientSecret((String)ccConfig.get(ClientConfig.CLIENT_SECRET));
                } else {
                    logger.error(new Status(CONFIG_PROPERTY_MISSING, "client_credentials client_secret", "client.yml").toString());
                }
                setUri((String)ccConfig.get(ClientConfig.URI));
                //set default scope from config.
                setScope((List<String>)ccConfig.get(ClientConfig.SCOPE));
                // overwrite server url, id, proxy host, id and http2 flag if they are defined in the ccConfig.
                // This is only used by the multiple auth servers. There is no reason to overwrite in single auth server.
                if(ccConfig.get(ClientConfig.SERVER_URL) != null) {
                    setServerUrl((String)ccConfig.get(ClientConfig.SERVER_URL));
                }
                if(ccConfig.get(ClientConfig.SERVICE_ID) != null) {
                    setServiceId((String)ccConfig.get(ClientConfig.SERVICE_ID));
                }
                if(ccConfig.get(ClientConfig.PROXY_HOST) != null) {
                    // give a chance to set proxyHost to null if a service doesn't need proxy.
                    String proxyHost = (String)ccConfig.get(ClientConfig.PROXY_HOST);
                    if(proxyHost.length() > 1) {
                        setProxyHost((String)ccConfig.get(ClientConfig.PROXY_HOST));
                        port = ccConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)ccConfig.get(ClientConfig.PROXY_PORT);
                        setProxyPort(port);
                    } else {
                        // overwrite the inherited proxyHost from the tokenConfig.
                        setProxyHost(null);
                        setProxyPort(0);
                    }
                }
                if(ccConfig.get(ClientConfig.ENABLE_HTTP2) != null) {
                    setEnableHttp2((Boolean)ccConfig.get(ClientConfig.ENABLE_HTTP2));
                }
            }
        }
    }
}
