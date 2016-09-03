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
