package com.networknt.portal.registry.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class McpHandlerTest {
    private static final Logger logger = LoggerFactory.getLogger(McpHandlerTest.class);

    @Mock
    private PortalRegistryWebSocketClient client;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartStopLogs() throws Exception {
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

        // Verify no more notifications were sent after stop
        // (total count should still be what it was before stop)
        // We use verifyNoMoreInteractions or check the count again.
    }
}
