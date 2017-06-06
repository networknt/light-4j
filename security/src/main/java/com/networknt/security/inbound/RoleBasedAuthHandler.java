package com.networknt.security.inbound;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Role-Based Authorization Handler is an abstract handler to define the structure of the fine-grained
 * Authorization within business context. We don't provide the final implementation as we don't have
 * the business knowledge to do so.
 *
 * Assume role will be passed in from JwT token as role with a list of role ids like scopes.
 *
 * @author Steve Hu
 */
public abstract class RoleBasedAuthHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

    }


}
