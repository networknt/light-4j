package com.networknt.handler.config;

import com.networknt.handler.MiddlewareHandler;
import com.networknt.service.ServiceUtil;
import io.undertow.server.HttpHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Azar
 */
public class HandlerConfigValidator {
    private static final List<String> validHttpVerbs = new ArrayList<String>() {{
        add("get");add("post");add("put");add("delete");add("options");add("head");add("connect");add("trace");add("patch");
    }};

    /**
     * - All middleware are instances of MiddlewareHandler.
     * - All endpoints are instances of HttpHandler.
     * - All namedMiddlewareChains are defined.
     * - If named middleware is defined, middleware list isn't
     * - All httpVerbs are a correct option.
     * - Paths aren't duplicated in the same handler.
     * - Named middleware names aren't duplicated.
     */
    public static void validate(HandlerConfig handlerConfig) throws Exception {
        for (PathHandler pathHandler : handlerConfig.getPathHandlers()) {
            for (HandlerPath handlerPath : pathHandler.getPaths()) {
                // check that all middleware are instances of MiddlewareHandler.
                if (handlerPath.getMiddleware() != null) {
                    for (Object middlewareConfig : handlerPath.getMiddleware()) {
                        if (!(ServiceUtil.construct(middlewareConfig) instanceof MiddlewareHandler)) {
                            throw new Exception("Middleware configured for path \"" + handlerPath.getPath() + "\" is not an instance of MiddlewareHandler: " + middlewareConfig);
                        }
                    }
                }

                if (handlerPath.getNamedMiddlewareChain() != null && handlerPath.getNamedMiddlewareChain().length() > 0) {
                    // check that if named middleware is defined, middleware list isn't.
                    if (handlerPath.getMiddleware() != null && handlerPath.getMiddleware().size() > 0) {
                        throw new Exception("Named middleware and middleware list can't both be configured for a path: " + handlerPath.getPath());
                    }

                    // check that all namedMiddlewareChains are defined and not duplicated
                    if (handlerConfig.getNamedMiddlewareChain() != null && handlerConfig.getNamedMiddlewareChain().size() > 0) {
                        if (handlerConfig.getNamedMiddlewareChain().stream().filter(chain -> chain.getName().equals(handlerPath.getNamedMiddlewareChain())).count() != 1) {
                            throw new Exception("Exactly 1 named middleware chain needs to match name in path: " + handlerPath.getPath() + " chain name: " + handlerPath.getNamedMiddlewareChain());
                        }
                    } else {
                        throw new Exception("Named middleware chain configured by config has none. " + handlerPath.getPath() + " chain name: " + handlerPath.getNamedMiddlewareChain());
                    }
                }

                // check that all httpVerbs are a correct option.
                if (!(validHttpVerbs.contains(handlerPath.getHttpVerb()))) {
                    throw new Exception("Invalid http verb provided: " + handlerPath.getHttpVerb());
                }

                // check that all endpoints are instances of HttpHandler.
                if (!(ServiceUtil.construct(handlerPath.getEndPoint()) instanceof HttpHandler)) {
                    throw new Exception("Endpoints need to be instances of HttpHandler: " + handlerPath.getEndPoint());
                }

                // check that paths aren't duplicated in the same handler.
                if (pathHandler.getPaths().stream().filter(handlerPath1 -> handlerPath1.getPath().equals(handlerPath.getPath()) && handlerPath1.getHttpVerb().equals(handlerPath.getHttpVerb())).count() != 1) {
                    throw new Exception("Found more then one matching path with the same http verb: " + handlerPath.getPath() + "@" + handlerPath.getHttpVerb());
                }
            }
        }


    }
}
