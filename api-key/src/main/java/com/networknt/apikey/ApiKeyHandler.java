package com.networknt.apikey;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.HashUtil;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For some legacy applications to migrate from the monolithic gateway to light-gateway without changing
 * any code, we need to support the API Key authentication on the light-gateway(LG) or light-client-proxy(LCP)
 * to authenticate the consumer and then change the authentication from API Key to OAuth 2.0 for downstream
 * API access.
 *
 * Only certain paths will have API Key set up and the header name for each application might be different. To
 * support all use cases, we add a list of maps to the configuration apikey.yml to pathPrefixAuths property.
 *
 * Each config item will have pathPrefix, headerName and apiKey. The handler will try to match the path prefix
 * first and then get the input API Key from the header. After compare with the configured API Key, the handler
 * will return either ERR10057 API_KEY_MISMATCH or pass the control to the next handler in the chain.
 *
 * @author Steve Hu
 */
public class ApiKeyHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ApiKeyHandler.class);
    static final String API_KEY_MISMATCH = "ERR10075";

    private volatile HttpHandler next;
    private volatile String configName = ApiKeyConfig.CONFIG_NAME;

    public ApiKeyHandler() {
        // Force to load the config and register it during the server startup, and force to load the test resource.
        ApiKeyConfig.load(configName);
        if(logger.isTraceEnabled()) logger.trace("ApiKeyHandler is loaded.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     * @param configName String
     */
    @Deprecated
    public ApiKeyHandler(String configName) {
        this.configName = configName;
        // Force to load the config and register it during the server startup, and force to load the test resource.
        ApiKeyConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("ApiKeyHandler is loaded.");
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return ApiKeyConfig.load(configName).isEnabled();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest starts.");
        String requestPath = exchange.getRequestPath();
        if(handleApiKey(exchange, requestPath)) {
            if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest ends.");
            // only goes to the next handler the APIKEY verification is passed successfully.
            Handler.next(exchange, next);
        }
    }

    public boolean handleApiKey(HttpServerExchange exchange, String requestPath) {
        if(logger.isTraceEnabled()) logger.trace("requestPath = {}", requestPath);
        ApiKeyConfig config = ApiKeyConfig.load(configName);
        if (config.getPathPrefixAuths() != null) {
            boolean matched = false;
            boolean found = false;
            // iterate all the ApiKey entries to find if any of them matches the request path.
            for(ApiKey apiKey: config.getPathPrefixAuths()) {
                if(requestPath.startsWith(apiKey.getPathPrefix())) {
                    found = true;
                    // found the matched prefix, validate the apiKey by getting the header and compare.
                    String k = exchange.getRequestHeaders().getFirst(apiKey.getHeaderName());
                    if(config.hashEnabled) {
                        // hash the apiKey and compare with the one in the config.
                        try {
                            matched = HashUtil.validatePassword(k.toCharArray(), apiKey.getApiKey());
                            if(matched) {
                                if (logger.isTraceEnabled())
                                    logger.trace("Found valid apiKey with prefix = {} headerName = {}", apiKey.getPathPrefix(), apiKey.getHeaderName());
                                break;
                            }
                        } catch (Exception e) {
                            // there is no way to get here as the validatePassword will not throw any exception.
                            logger.error("Exception:", e);
                        }
                    } else {
                        // if not hash enabled, then compare the apiKey directly.
                        if(apiKey.getApiKey().equals(k)) {
                            if (logger.isTraceEnabled())
                                logger.trace("Found matched apiKey with prefix = {} headerName = {}", apiKey.getPathPrefix(), apiKey.getHeaderName());
                            matched = true;
                            break;
                        }
                    }
                }
            }
            if(!found) {
                // the request path is no in the configuration, consider pass and go to the next handler.
                return true;
            }
            if(!matched) {
                // at this moment, if not matched, then return an error message.
                logger.error("Could not find matched APIKEY for request path {}", requestPath);
                setExchangeStatus(exchange, API_KEY_MISMATCH, requestPath);
                if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest ends with an error.");
                exchange.endExchange();
                return false;
            }
        }
        return true;
    }
}
