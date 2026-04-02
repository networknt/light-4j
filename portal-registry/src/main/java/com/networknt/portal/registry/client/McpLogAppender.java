package com.networknt.portal.registry.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * McpLogAppender is a Logback appender that sends logging events as JSON-RPC notifications
 * over the PortalRegistry WebSocket connection.
 */
public class McpLogAppender extends AppenderBase<ILoggingEvent> {
    private final PortalRegistryWebSocketClient client;
    private final Level threshold;

    public McpLogAppender(PortalRegistryWebSocketClient client, Level threshold) {
        this.client = client;
        this.threshold = threshold;
    }

    public boolean isForClient(PortalRegistryWebSocketClient websocketClient) {
        return this.client == websocketClient;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (client == null || !client.isOpen()) {
            return;
        }
        if (threshold != null && !event.getLevel().isGreaterOrEqual(threshold)) {
            return;
        }

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("timestamp", event.getTimeStamp());
        log.put("level", event.getLevel().toString());
        log.put("logger", event.getLoggerName());
        log.put("message", event.getFormattedMessage());
        log.put("thread", event.getThreadName());

        if (event.getThrowableProxy() != null) {
            log.put("exception", event.getThrowableProxy().getMessage());
        }

        try {
            client.sendNotification("notifications/log", log);
        } catch (Exception e) {
            // Silently ignore log transport errors to avoid infinite recursion
        }
    }
}
