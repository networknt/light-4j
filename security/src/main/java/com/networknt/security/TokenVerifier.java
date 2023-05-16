package com.networknt.security;

import com.networknt.client.ClientConfig;
import com.networknt.config.ConfigException;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

public class TokenVerifier {
    static final Logger logger = LoggerFactory.getLogger(TokenVerifier.class);
    protected Map<String, Object> getJwkConfig(ClientConfig clientConfig, String serviceId) {
        if (logger.isTraceEnabled())
            logger.trace("serviceId = " + serviceId);
        // get the serviceIdAuthServers for key definition
        Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
        Map<String, Object> keyConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.KEY);
        Map<String, Object> serviceIdAuthServers = (Map<String, Object>) keyConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
        if (serviceIdAuthServers == null) {
            throw new ConfigException("serviceIdAuthServers property is missing in the token key configuration in client.yml");
        }
        return (Map<String, Object>) serviceIdAuthServers.get(serviceId);
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

    /**
     * Checks to see if the current exchange type is Upgrade.
     * Two conditions required for a valid upgrade request.
     * - 'Connection' header is set to 'upgrade'.
     * - 'Upgrade' is present.
     *
     * @param headerMap - map containing all exchange headers
     * @return - returns true if the request is an Upgrade request.
     */
    public boolean checkForH2CRequest(HeaderMap headerMap) {
        return  headerMap.getFirst(Headers.UPGRADE) != null
                && headerMap.getFirst(Headers.CONNECTION) != null
                && headerMap.getFirst(Headers.CONNECTION).equalsIgnoreCase("upgrade");
    }

}
