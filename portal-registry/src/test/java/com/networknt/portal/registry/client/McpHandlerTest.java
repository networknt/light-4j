package com.networknt.portal.registry.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class McpHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(McpHandlerTest.class);

    @Mock
    private PortalRegistryWebSocketClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testStartStopLogs() {
        // Prepare start_logs request
        Map<String, Object> params = new HashMap<>();
        params.put("name", "start_logs");
        Map<String, Object> args = new HashMap<>();
        args.put("level", "INFO");
        params.put("arguments", args);

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 123);
        envelope.put("params", params);

        // Ensure client is "open" for the appender
        when(client.isOpen()).thenReturn(true);

        // Call start_logs
        McpHandler.handle(client, envelope);

        // Verify result was sent
        verify(client).sendResult(eq(123), any());

        // Log a message to trigger the appender
        logger.info("Test log message for MCP");

        // Verify notification was sent by the appender
        // Note: There might be a slight delay or buffering, but AppenderBase.append is synchronous
        verify(client, atLeastOnce()).sendNotification(eq("notifications/log"), any());
        clearInvocations(client);

        // Call stop_logs
        Map<String, Object> stopParams = new HashMap<>();
        stopParams.put("name", "stop_logs");
        Map<String, Object> stopEnvelope = new HashMap<>();
        stopEnvelope.put("method", "tools/call");
        stopEnvelope.put("id", 124);
        stopEnvelope.put("params", stopParams);

        McpHandler.handle(client, stopEnvelope);

        // Verify stop result
        verify(client).sendResult(eq(124), any());

        // Log another message
        logger.info("This should not be sent");

        verify(client, never()).sendNotification(eq("notifications/log"), any());
    }

    @Test
    void testToolsCallRejectsMissingParams() {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 125);

        McpHandler.handle(client, envelope);

        verify(client).sendError(eq(125), eq(-32602), contains("Missing or invalid params"));
    }

    @Test
    void testStopLogsForClientDetachesOnDisconnect() {
        // Start log streaming with this client
        when(client.isOpen()).thenReturn(true);
        Map<String, Object> params = new HashMap<>();
        params.put("name", "start_logs");
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 200);
        envelope.put("params", params);
        McpHandler.handle(client, envelope);

        // Log a message to confirm appender is active
        logger.info("Before disconnect");
        verify(client, atLeastOnce()).sendNotification(eq("notifications/log"), any());
        clearInvocations(client);

        // Simulate websocket disconnect by calling stopLogsForClient
        McpHandler.stopLogsForClient(client);

        // Log after disconnect — appender should be detached
        logger.info("After disconnect, should not be sent");
        verify(client, never()).sendNotification(eq("notifications/log"), any());
    }

    @Test
    void testStopLogsForClientDoesNotStopUnrelatedClient() {
        PortalRegistryWebSocketClient otherClient = mock(PortalRegistryWebSocketClient.class);
        when(otherClient.isOpen()).thenReturn(true);

        // Start logs for otherClient
        Map<String, Object> params = new HashMap<>();
        params.put("name", "start_logs");
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 300);
        envelope.put("params", params);
        McpHandler.handle(otherClient, envelope);

        // Verify appender is active for otherClient
        logger.info("Sent via otherClient appender");
        verify(otherClient, atLeastOnce()).sendNotification(eq("notifications/log"), any());
        clearInvocations(otherClient);

        // Simulate disconnect of the original (unrelated) client — should NOT stop otherClient's appender
        McpHandler.stopLogsForClient(client);

        // Log again — otherClient's appender should still be active
        logger.info("Still active after unrelated disconnect");
        verify(otherClient, atLeastOnce()).sendNotification(eq("notifications/log"), any());

        // Cleanup
        McpHandler.stopLogsForClient(otherClient);
    }

    @Test
    void testExplicitCloseDetachesActiveLogAppender() {
        PortalRegistryWebSocketClient realClient =
                new PortalRegistryWebSocketClient(URI.create("wss://localhost:8443/ws/microservice"), null);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "start_logs");
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 400);
        envelope.put("params", params);

        McpHandler.handle(realClient, envelope);

        realClient.close();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        boolean hasMcpAppender = false;
        for (Iterator<Appender<ILoggingEvent>> iterator = root.iteratorForAppenders(); iterator.hasNext(); ) {
            if (iterator.next() instanceof McpLogAppender) {
                hasMcpAppender = true;
                break;
            }
        }

        org.junit.jupiter.api.Assertions.assertFalse(hasMcpAppender);
    }

    @Test
    void testStartLogsFiltersBelowThreshold() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "start_logs");
        params.put("arguments", Map.of("level", "ERROR"));
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 500);
        envelope.put("params", params);

        when(client.isOpen()).thenReturn(true);

        McpHandler.handle(client, envelope);
        clearInvocations(client);

        logger.info("Below threshold");
        verify(client, never()).sendNotification(eq("notifications/log"), any());

        logger.error("At threshold");
        verify(client, atLeastOnce()).sendNotification(eq("notifications/log"), any());

        McpHandler.stopLogsForClient(client);
    }

    @Test
    void testGetLogContentRejectsInvalidLoggerLevelType() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "get_log_content");
        params.put("arguments", Map.of(
                "startTime", 0L,
                "loggerLevel", true
        ));
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("method", "tools/call");
        envelope.put("id", 501);
        envelope.put("params", params);

        McpHandler.handle(client, envelope);

        verify(client).sendError(eq(501), eq(-32602), contains("Invalid logger level parameter"));
    }
}
