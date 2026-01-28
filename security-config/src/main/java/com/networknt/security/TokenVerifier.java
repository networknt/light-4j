package com.networknt.security;

import com.networknt.client.AuthServerConfig;
import com.networknt.client.ClientConfig;
import com.networknt.client.OAuthTokenConfig;
import com.networknt.client.OAuthTokenKeyConfig;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

public class TokenVerifier {
    private static final Logger logger = LoggerFactory.getLogger(TokenVerifier.class);
    protected AuthServerConfig getJwkConfig(ClientConfig clientConfig, String serviceId) {
        if (logger.isTraceEnabled()) logger.trace("serviceId = {}", serviceId);
        // get the serviceIdAuthServers for key definition
        OAuthTokenConfig tokenConfig = clientConfig.getOAuth().getToken();
        OAuthTokenKeyConfig tokenKeyConfig = tokenConfig.getKey();
        Map<String, AuthServerConfig> serviceIdAuthServers = tokenKeyConfig.getServiceIdAuthServers();
        if (serviceIdAuthServers == null) {
            throw new ConfigException("serviceIdAuthServers property is missing in the token key configuration in client.yml");
        }
        return serviceIdAuthServers.get(serviceId);
    }

    /**
     * Parse the jwt or swt token from Authorization header.
     *
     * @param authorization authorization header.
     * @return JWT or SWT token
     */
    public static String getTokenFromAuthorization(String authorization) {
        String token = null;
        if (authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    token = credentials;
                }
            }
        }
        return token;
    }
}
