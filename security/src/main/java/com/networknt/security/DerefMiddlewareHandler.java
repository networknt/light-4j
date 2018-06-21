package com.networknt.security;

import com.networknt.client.oauth.DerefRequest;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This is the middleware handler that is responsible for sending the by reference token to OAuth 2.0
 * provider to exchange to a JWT in order to satisfy the light framework requirement. This is usually
 * used on the BFF which is a static access point for the external clients. For some organizations, they
 * would not send the JWT token to the Internet but only the by reference token to the outside. However,
 * internally we need JWT token to access APIs or services. This handler can work with one OAuth 2.0
 * provider or two OAuth 2.0 providers (External OAuth 2.0 and Internal OAuth 2.0)
 *
 * @author Steve Hu
 */
public class DerefMiddlewareHandler implements MiddlewareHandler {

    private static final Logger logger = LoggerFactory.getLogger(DerefMiddlewareHandler.class);
    private static final String CONFIG_NAME = "deref";
    private static final String MISSING_AUTH_TOKEN = "ERR10002";
    private static final String EMPTY_TOKEN_DEREFERENCE_RESPONSE = "ERR10044";
    private static final String TOKEN_DEREFERENCE_ERROR = "ERR10045";

    public static DerefConfig config =
            (DerefConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, DerefConfig.class);

    private volatile HttpHandler next;

    public DerefMiddlewareHandler() {
        if(logger.isInfoEnabled()) logger.info("DerefMiddlewareHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // check if the token is in the request Authorization header
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if(token == null) {
            setExchangeStatus(exchange, MISSING_AUTH_TOKEN);
            return;
        } else {
            // ignore it and let it go if the token format is JWT
            if(token.indexOf('.') < 0) {
                // this is a by reference token
                DerefRequest request = new DerefRequest(token);
                String response = OauthHelper.derefToken(request);
                if(response == null || response.trim().length() == 0) {
                    setExchangeStatus(exchange, EMPTY_TOKEN_DEREFERENCE_RESPONSE, token);
                    return;
                }
                if(response.startsWith("{")) {
                    // an error status returned from OAuth 2.0 provider. We cannot assume that light-oauth2
                    // is used but still need to convert the error message to a status to wrap the error.
                    setExchangeStatus(exchange, TOKEN_DEREFERENCE_ERROR, response);
                    return;
                } else {
                    // now consider the response it jwt
                    exchange.getRequestHeaders().put(Headers.AUTHORIZATION, response);
                }
            }
        }
        next.handleRequest(exchange);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
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
        ModuleRegistry.registerModule(DerefMiddlewareHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
