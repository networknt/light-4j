package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The request that is used to post the introspection request to verify the simple web token. It first checks
 * the token/key section in the client.yml and then key section of token for backward compatibility. It is
 * recommended to set the key/introspection under token to clear indicate that the introspection is for token
 * verification.
 *
 * @author Steve Hu
 */
public class TokenIntrospectionRequest extends IntrospectionRequest {
    private static Logger logger = LoggerFactory.getLogger(TokenIntrospectionRequest.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";

    public TokenIntrospectionRequest(String swt) {
        this(swt, null);
    }
    public TokenIntrospectionRequest(String swt, Map<String, Object> introspectionConfig) {
        super(swt);
        Map<String, Object> clientConfig = ClientConfig.get().getMappedConfig();
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(ClientConfig.OAUTH);
            if(oauthConfig != null) {
                // there is no key section under oauth. look up in the oauth/token section for introspection
                Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
                if(tokenConfig != null) {
                    // first inherit the proxy config from the token config.
                    setProxyHost((String)tokenConfig.get(ClientConfig.PROXY_HOST));
                    int port = tokenConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)tokenConfig.get(ClientConfig.PROXY_PORT);
                    setProxyPort(port);
                    if(introspectionConfig == null) introspectionConfig = (Map<String, Object>)tokenConfig.get(ClientConfig.KEY);
                    if(introspectionConfig != null) {
                        setIntrospectionOptions(introspectionConfig);
                    } else {
                        logger.error(new Status(CONFIG_PROPERTY_MISSING, "token section", "client.yml").toString());
                    }
                } else {
                    logger.error(new Status(CONFIG_PROPERTY_MISSING, "token section", "client.yml").toString());
                }
            } else {
                logger.error(new Status(CONFIG_PROPERTY_MISSING, "oauth section", "client.yml").toString());
            }
        } else {
            logger.error(new Status(CONFIG_PROPERTY_MISSING, "oauth key section", "client.yml").toString());
        }
    }

    private void setIntrospectionOptions(Map<String, Object> introspectionConfig) {
        setServerUrl((String)introspectionConfig.get(ClientConfig.SERVER_URL));
        setServiceId((String)introspectionConfig.get(ClientConfig.SERVICE_ID));
        Object object = introspectionConfig.get(ClientConfig.ENABLE_HTTP2);
        setEnableHttp2(object != null && (Boolean) object);
        setUri(introspectionConfig.get(ClientConfig.URI).toString());

        // clientId is optional
        if(introspectionConfig.get(ClientConfig.CLIENT_ID) != null) {
            setClientId((String)introspectionConfig.get(ClientConfig.CLIENT_ID));
        }
        // clientSecret is optional
        if(introspectionConfig.get(ClientConfig.CLIENT_SECRET) != null) {
            setClientSecret((String)introspectionConfig.get(ClientConfig.CLIENT_SECRET));
        }
        // proxyHost and proxyPort are optional to overwrite the token config inherited.
        if(introspectionConfig.get(ClientConfig.PROXY_HOST) != null) {
            String proxyHost = (String)introspectionConfig.get(ClientConfig.PROXY_HOST);
            if(proxyHost.length() > 1) {
                // overwrite the tokenConfig proxyHost and proxyPort if this particular service has different proxy server
                setProxyHost(proxyHost);
                int port = introspectionConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)introspectionConfig.get(ClientConfig.PROXY_PORT);
                setProxyPort(port);
            } else {
                // if this service doesn't need a proxy server, just use an empty string to remove the tokenConfig proxy host.
                setProxyHost(null);
                setProxyPort(0);
            }
        }
    }

}
