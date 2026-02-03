package com.networknt.sse;

import io.undertow.server.handlers.sse.ServerSentEventConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SseConnectionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SseConnectionRegistry.class);
    private static final Map<String, ServerSentEventConnection> connections = new ConcurrentHashMap<>();

    private SseConnectionRegistry() {
    }

    public static void add(String id, ServerSentEventConnection connection) {
        if(logger.isTraceEnabled()) logger.trace("add connection for id {}", id);
        connections.put(id, connection);
    }

    public static void remove(String id) {
        if(logger.isTraceEnabled()) logger.trace("remove connection for id {}", id);
        ServerSentEventConnection connection = connections.remove(id);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                logger.error("Error closing connection for id {}", id, e);
            }
        }
    }

    public static ServerSentEventConnection getConnection(String id) {
        return connections.get(id);
    }
    
    public static Map<String, ServerSentEventConnection> getConnections() {
        return connections;
    }
}
