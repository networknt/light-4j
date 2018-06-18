package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.handler.config.HandlerConfigValidator;
import com.networknt.handler.config.HandlerPath;
import com.networknt.handler.config.NamedMiddlewareChain;
import com.networknt.service.ServiceUtil;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicholas Azar
 */
public class PathMiddlewareHandler implements NonFunctionalMiddlewareHandler {

    private static final String CONFIG_NAME = "handler";
    public static HandlerConfig config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HandlerConfig.class);
    private volatile HttpHandler next;
    private String handlerName;

    @Override
    /**
     * Since this class implements NonFunctionalMiddlewareHandler, instead of this being called, the handler returned in next will be instead.
     */
    public void handleRequest(HttpServerExchange exchange) throws Exception {}

    /**
     * @return The list of handler paths that have been configured by the current handler name.
     */
    List<HandlerPath> getHandlerPaths() {
        return config.getPathHandlers().stream()
                .filter(pathHandler -> pathHandler.getHandlerName().equals(handlerName))
                .filter(pathHandler -> pathHandler.getPaths() != null && pathHandler.getPaths().size() > 0)
                .flatMap(pathHandler -> pathHandler.getPaths().stream())
                .collect(Collectors.toList());
    }

    /**
     * Return a handler chain built out of the given end point and middleware configuration.
     * @param endPointConfig The configuration to build an endpoint, follows service.yml format.
     * @param middlewareConfigList The configuration to build the middleware chain.
     * @return An HttpHandler entering at the first middleware and ending at the endpoint.
     */
    private HttpHandler getHandler(Object endPointConfig, List<Object> middlewareConfigList) {
        HttpHandler httpHandler = null;
        try {
            Object object = ServiceUtil.construct(endPointConfig);
            if (object instanceof HttpHandler) {
                httpHandler = (HttpHandler) object;
                // If they give an empty middleware list, we want to only return the endpoint.
                if (middlewareConfigList != null && middlewareConfigList.size() > 0) {
                    List<Object> reverseOrderedMiddlewareConfigList = new ArrayList<>(middlewareConfigList);
                    Collections.reverse(reverseOrderedMiddlewareConfigList);
                    for (Object middleware : reverseOrderedMiddlewareConfigList) {
                        Object constructedMiddleware = ServiceUtil.construct(middleware);
                        if (constructedMiddleware instanceof MiddlewareHandler) {
                            MiddlewareHandler middlewareHandler = (MiddlewareHandler) constructedMiddleware;
                            if (middlewareHandler.isEnabled()) {
                                httpHandler = middlewareHandler.setNext(httpHandler);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed when retrieving Handler.", e);
        }
        return httpHandler;
    }

    /**
     * When a new instance is create, validate config. Right now it's ok not to fail when a validation error is thrown
     * and should be enough just to log an error. Eventually we may want to propagate this exception.
     */
    public PathMiddlewareHandler() {
        try {
            HandlerConfigValidator.validate(config);
        } catch (Exception e) {
            logger.error("Found validation errors in HandlerConfig", e);
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    /**
     * Build out the RoutingHandler from the given config.
     * @param next HttpHandler The next handler in the chain (could be another middleware handler).
     * @return this, with the next instance variable set.
     */
    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        RoutingHandler routingHandler = Handlers.routing().setFallbackHandler(next);
        for (HandlerPath handlerPath : getHandlerPaths()) {
            try {
                if (handlerPath.getNamedMiddlewareChain() == null || handlerPath.getNamedMiddlewareChain().length() == 0) {
                    HttpHandler httpHandler = getHandler(handlerPath.getEndPoint(), handlerPath.getMiddleware());
                    if (httpHandler != null) {
                        routingHandler.add(handlerPath.getHttpVerb(), handlerPath.getPath(), httpHandler);
                    }
                } else {
                    // Handle named request chains.
                    List<NamedMiddlewareChain> requestChains = config.getNamedMiddlewareChain().stream()
                            .filter(namedMiddlewareChain -> namedMiddlewareChain.getName().equals(handlerPath.getNamedMiddlewareChain())).collect(Collectors.toList());
                    if (requestChains != null && requestChains.size() > 0) {
                        HttpHandler httpHandler = getHandler(handlerPath.getEndPoint(), requestChains.get(0).getMiddleware());
                        if (httpHandler != null) {
                            routingHandler.add(handlerPath.getHttpVerb(), handlerPath.getPath(), httpHandler);
                        }
                    } else {
                        throw new Exception("Named request chain \"" + handlerPath.getNamedMiddlewareChain() + "\" not found in config");
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to add PathMiddlewareHandler.", e);
            }
        }
        this.next = routingHandler;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(HandlerConfig.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    // Exposed for testing only.
    protected void setConfig(String configName) {
        config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(configName, HandlerConfig.class);
        try {
            HandlerConfigValidator.validate(config);
        } catch (Exception e) {
            logger.error("Found validation errors in HandlerConfig", e);
        }
    }
}
