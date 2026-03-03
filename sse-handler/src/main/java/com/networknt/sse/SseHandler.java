package com.networknt.sse;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;

import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xnio.ChannelListener;

import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.Handler;
import io.undertow.server.HttpHandler;

public class SseHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(SseHandler.class);
    private volatile String configName = SseConfig.CONFIG_NAME;
    private volatile HttpHandler next;

    public SseHandler() {
        SseConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("SseHandler initialized.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     * @param configName String
     */
    @Deprecated
    public SseHandler(String configName) {
        this.configName = configName;
        SseConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("SseHandler initialized with config {}.", configName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.debug("SseHandler.handleRequest starts.");
        SseConfig config = SseConfig.load(configName);
        String path = exchange.getRequestPath();
        String matchedPath = null;
        int keepAliveInterval = config.getKeepAliveInterval();

        if (config.getPathPrefixes() != null) {
            for (PathPrefix pp : config.getPathPrefixes()) {
                if (path.startsWith(pp.getPathPrefix())) {
                    matchedPath = pp.getPathPrefix();
                    if (pp.getKeepAliveInterval() > 0) {
                        keepAliveInterval = pp.getKeepAliveInterval();
                    }
                    break;
                }
            }
        }

        if (matchedPath == null && path.equals(config.getPath())) {
            matchedPath = config.getPath();
        }

        final int finalKeepAliveInterval = keepAliveInterval;

        if (matchedPath != null) {
            ServerSentEventHandler sseHandler = new ServerSentEventHandler(new ServerSentEventConnectionCallback() {
                @Override
                public void connected(ServerSentEventConnection connection, String lastEventId) {
                    if (logger.isTraceEnabled()) logger.trace("connected with lastEventId {}", lastEventId);
                    if (finalKeepAliveInterval > 0) {
                        connection.setKeepAliveTime(finalKeepAliveInterval);
                    }
                    String id = null;
                    // Check if there is a header X-Traceability-Id
                    var headerValues = exchange.getRequestHeaders().get("X-Traceability-Id");
                    if (headerValues != null && !headerValues.isEmpty()) {
                        id = headerValues.getFirst();
                    } else {
                        // try to get the id from query parameter
                        if (exchange.getQueryParameters().get("id") != null) {
                            id = exchange.getQueryParameters().get("id").getFirst();
                        }
                    }
                    if (id == null) {
                        id = lastEventId;
                    }
                    if (id == null) {
                        id = java.util.UUID.randomUUID().toString();
                    }
                    if (id != null) {
                        SseConnectionRegistry.add(id, connection);
                        connection.addCloseTask(new ChannelListener<ServerSentEventConnection>() {
                            @Override
                            public void handleEvent(ServerSentEventConnection channel) {
                                // cannot get the id from the connection, so we have to use the id captured in the closure.
                                if (logger.isTraceEnabled()) logger.trace("closed connection for id {}", lastEventId);
                                String closedId = null;
                                // Check if there is a header X-Traceability-Id
                                var headerValues = exchange.getRequestHeaders().get("X-Traceability-Id");
                                if (headerValues != null && !headerValues.isEmpty()) {
                                    closedId = headerValues.getFirst();
                                } else {
                                    // try to get the id from query parameter
                                    if (exchange.getQueryParameters().get("id") != null) {
                                        closedId = exchange.getQueryParameters().get("id").getFirst();
                                    }
                                }
                                if(closedId == null) closedId = lastEventId;
                                if(closedId != null) SseConnectionRegistry.remove(closedId);
                            }
                        });
                    }
                }
            });
            sseHandler.handleRequest(exchange);
        } else {
            // This handler is not responsible for the path, so just call the next handler.
            if (logger.isDebugEnabled()) logger.debug("SseHandler.handleRequest ends.");
            Handler.next(exchange, next);
        }
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
        return SseConfig.load(configName).isEnabled();
    }
}
