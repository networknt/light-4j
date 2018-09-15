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


import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

/**
 * This holds values used to call the SAML Bearer grant flow from the OAuth Server.
 * In this version client presents a SAML token and a JWT token (requirment for project)
 *
 * @author David G.
 */
public class SAMLBearerRequest extends TokenRequest {

    static Map<String, Object> secret = DecryptUtil.decryptMap((Map<String, Object>) Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET));
    // x-www-urlencoded keys / values sent to OAuth server for SAML grant flow
    static final String CLIENT_ASSERTION_TYPE_KEY = "client_assertion_type";
    static final String CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    static final String CLIENT_ASSERTION_KEY = "client_assertion"; // value is JWT
    static final String ASSERTION_KEY = "assertion"; // value is SAML token
    static final String GRANT_TYPE_KEY="grant_type";
    static final String GRANT_TYPE_VALUE="urn:ietf:params:oauth:grant-type:saml2-bearer";

    private String samlAssertion = "";
    private String jwtClientAssertion = "";


    public SAMLBearerRequest(String samlAssertion , String jwtClientAssertion) {

        setGrantType(SAML_BEARER);
        this.samlAssertion = samlAssertion ;
        this.jwtClientAssertion = jwtClientAssertion ;

        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
                if(tokenConfig != null) {
                    setServerUrl((String)tokenConfig.get(SERVER_URL));
                    Object object = tokenConfig.get(ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    Map<String, Object> ccConfig = (Map<String, Object>) tokenConfig.get(CLIENT_CREDENTIALS);
                    if(ccConfig != null) {
                        setClientId((String)ccConfig.get(CLIENT_ID));
                        setClientSecret((String)secret.get(SecretConstants.CLIENT_CREDENTIALS_CLIENT_SECRET));
                        setUri((String)ccConfig.get(URI));
                        setScope((List<String>)ccConfig.get(SCOPE));
                    }
                }
            }
        }
    }

    public String getSamlAssertion(){
        return this.samlAssertion ;
    }

    public String getJwtClientAssertion(){
        return this.jwtClientAssertion ;
    }
}
