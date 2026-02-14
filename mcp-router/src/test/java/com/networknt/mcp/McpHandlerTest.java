package com.networknt.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import com.networknt.sse.SseConnectionRegistry;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class McpHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(McpHandlerTest.class);
    static Undertow server = null;
    static Undertow backendServer = null;
    static final int PORT = 7080;
    static final int BACKEND_PORT = 7081;

    @BeforeClass
    public static void setUp() {
        if (server == null) {
            logger.info("starting server");
            McpHandler handler = new McpHandler();
            // Manually register tool for test reliability
            McpTool weatherTool = new HttpMcpTool("weather", "Get weather information", "http://localhost:" + BACKEND_PORT, "/weather", "GET", null);
            McpToolRegistry.registerTool(weatherTool);

            handler.setNext(Handlers.routing().add(Methods.GET, "/health", exchange -> exchange.getResponseSender().send("OK")));
            server = Undertow.builder()
                    .addHttpListener(PORT, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
        if (backendServer == null) {
            logger.info("starting backend server");
            backendServer = Undertow.builder()
                    .addHttpListener(BACKEND_PORT, "localhost")
                    .setHandler(exchange -> {
                        if (exchange.getRequestPath().equals("/weather")) {
                            exchange.getResponseHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send("{\"temperature\": 25, \"unit\": \"C\"}");
                        } else if (exchange.getRequestPath().equals("/mcp-backend")) {
                            // Mock MCP backend behavior
                            exchange.getRequestReceiver().receiveFullString((exch, message) -> {
                                // Just echo back a success result
                                String response = "{\"jsonrpc\": \"2.0\", \"result\": {\"content\": [{\"type\": \"text\", \"text\": \"echo from mcp backend\"}]}, \"id\": " + System.currentTimeMillis() + "}";
                                exch.getResponseHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
                                exch.getResponseSender().send(response);
                            });
                        } else {
                            exchange.setStatusCode(404);
                        }
                    })
                    .build();
            backendServer.start();
        }
    }

    @Test
    public void testToolCall() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:" + PORT), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"weather\", \"arguments\": {}}, \"id\": 3}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(5000, TimeUnit.MILLISECONDS);
            int statusCode = reference.get().getResponseCode();
            Assert.assertEquals(200, statusCode);
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            Assert.assertNotNull(body);
            // Verify result contains weather data
            Assert.assertTrue(body.contains("temperature"));
            Assert.assertTrue(body.contains("25"));
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw e;
        } finally {

            client.restore(token);

        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (backendServer != null) {
            backendServer.stop();
        }
        // internal clear method or just rely on static?
        // McpToolRegistry doesn't have a clear method public exposed usually?
        // Let's check imports. We need to import HttpMcpTool first.
        McpToolRegistry.clear();
    }

    @Test
    public void testSseConnection() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:" + PORT), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.ACCEPT, "text/event-stream");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assert.assertEquals(200, response.getResponseCode());

            // Wait a bit for server handling
            Thread.sleep(500);
            Assert.assertFalse(SseConnectionRegistry.getConnections().isEmpty());

        } finally {


            client.restore(token);


        }
    }

    @Test
    public void testInitialize() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:" + PORT), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"initialize\", \"params\": {}, \"id\": 1}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assert.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            ObjectMapper mapper = Config.getInstance().getMapper();
            Map<String, Object> map = mapper.readValue(body, Map.class);
            Assert.assertEquals("2.0", map.get("jsonrpc"));
            Assert.assertEquals(1, map.get("id"));
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            Assert.assertNotNull(result);
            Assert.assertEquals("2024-11-05", result.get("protocolVersion"));
        } finally {

            client.restore(token);

        }
    }

    @Test
    public void testListTools() throws Exception {
        // Register a dummy tool first
        McpTool tool = new McpTool() {
            @Override
            public String getName() { return "testTool"; }
            @Override
            public String getDescription() { return "A test tool"; }
            @Override
            public String getInputSchema() { return "{\"type\": \"object\"}"; }
            @Override
            public Map<String, Object> execute(Map<String, Object> arguments) { return null; }
        };
        McpToolRegistry.registerTool(tool);

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:" + PORT), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"tools/list\", \"params\": {}, \"id\": 2}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assert.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            ObjectMapper mapper = Config.getInstance().getMapper();
            Map<String, Object> map = mapper.readValue(body, Map.class);
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            java.util.List<Map<String, Object>> tools = (java.util.List<Map<String, Object>>) result.get("tools");
            Assert.assertFalse(tools.isEmpty());
            boolean testToolFound = false;
            boolean weatherToolFound = false;
            for(Map<String, Object> toolMap: tools) {
                if("testTool".equals(toolMap.get("name"))) testToolFound = true;
                if("weather".equals(toolMap.get("name"))) weatherToolFound = true;
            }
            Assert.assertTrue(testToolFound);
            Assert.assertTrue(weatherToolFound);

        } finally {


            client.restore(token);


        }
    }

    @Test
    public void testMcpProxy() throws Exception {
        // Register an MCP proxy tool
        McpTool proxyTool = new McpProxyTool("mcpTool", "Proxy to backend MCP", "http://localhost:" + BACKEND_PORT, "/mcp-backend", "POST", null);
        McpToolRegistry.registerTool(proxyTool);

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:" + PORT), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"mcpTool\", \"arguments\": {}}, \"id\": 4}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assert.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            Assert.assertNotNull(body);
            // Verify result contains echo from backend
            Assert.assertTrue(body.contains("echo from mcp backend"));

        } finally {


            client.restore(token);


        }
    }
}
