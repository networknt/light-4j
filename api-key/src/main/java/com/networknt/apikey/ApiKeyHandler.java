package com.networknt.apikey;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    ApiKeyConfig config;

    private volatile HttpHandler next;

    public ApiKeyHandler() {
        if(logger.isTraceEnabled()) logger.trace("ApiKeyHandler is loaded.");
        config = ApiKeyConfig.load();
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     * @param cfg BasicAuthConfig
     */
    @Deprecated
    public ApiKeyHandler(ApiKeyConfig cfg) {
        config = cfg;
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        // As apiKeys are in the config file, we need to mask them.
        List<String> masks = new ArrayList<>();
        masks.add("apiKey");
        ModuleRegistry.registerModule(ApiKeyHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(ApiKeyConfig.CONFIG_NAME), masks);
    }

    @Override
    public void reload() {
        config = ApiKeyConfig.load();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest starts.");
        String requestPath = exchange.getRequestPath();
        handleApiKey(exchange, requestPath);
        if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    public void handleApiKey(HttpServerExchange exchange, String requestPath) {
        if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath);
        if (config.getPathPrefixAuths() != null) {
            // iterate all the ApiKey entries to find if any of them matches the request path.
            for(ApiKey apiKey: config.getPathPrefixAuths()) {
                if(requestPath.startsWith(apiKey.getPathPrefix())) {
                    // found the matched prefix, validate the apiKey by getting the header and compare.
                    String k = exchange.getRequestHeaders().getFirst(apiKey.getHeaderName());
                    if(apiKey.getApiKey().equals(k)) {
                        if(logger.isTraceEnabled()) logger.trace("Found matched apiKey with prefix = " + apiKey.getPathPrefix() + " headerName = " + apiKey.getHeaderName());
                        break;
                    } else {
                        logger.error("APIKEY from header " + apiKey.getHeaderName() + " is not matched for path prefix " + apiKey.getPathPrefix());
                        setExchangeStatus(exchange, API_KEY_MISMATCH, apiKey.getHeaderName(), apiKey.getPathPrefix());
                        if(logger.isDebugEnabled()) logger.debug("ApiKeyHandler.handleRequest ends with an error.");
                        exchange.endExchange();
                        return;
                    }
                }
            }
        }
    }
}
