package com.networknt.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.rule.Rule;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleEngine;
import com.networknt.rule.RuleExecutor;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import com.networknt.sse.SseConnectionRegistry;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
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
    static final TestRuleExecutor testRuleExecutor = new TestRuleExecutor();

    @BeforeAll
    public static void setUp() {
        SingletonServiceFactory.setBean(RuleExecutor.class.getName(), testRuleExecutor);
        if (server == null) {
            logger.info("starting server");
            McpHandler handler = new McpHandler();
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
        Assertions.assertNotNull(McpToolRegistry.getTool("weather"));

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
            Assertions.assertEquals(200, statusCode);
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            Assertions.assertNotNull(body);
            // Verify result contains weather data
            Assertions.assertTrue(body.contains("temperature"));
            Assertions.assertTrue(body.contains("25"));
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw e;
        } finally {

            client.restore(token);

        }
    }

    @AfterAll
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
        testRuleExecutor.setEndpointRules(new HashMap<>());
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
            Assertions.assertEquals(200, response.getResponseCode());

            // Wait a bit for server handling
            Thread.sleep(500);
            Assertions.assertFalse(SseConnectionRegistry.getConnections().isEmpty());

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
            Assertions.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            ObjectMapper mapper = Config.getInstance().getMapper();
            Map<String, Object> map = mapper.readValue(body, Map.class);
            Assertions.assertEquals("2.0", map.get("jsonrpc"));
            Assertions.assertEquals(1, map.get("id"));
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            Assertions.assertNotNull(result);
            Assertions.assertEquals("2024-11-05", result.get("protocolVersion"));
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
            public String getEndpoint() { return "testTool@call"; }
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
            Assertions.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            ObjectMapper mapper = Config.getInstance().getMapper();
            Map<String, Object> map = mapper.readValue(body, Map.class);
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            java.util.List<Map<String, Object>> tools = (java.util.List<Map<String, Object>>) result.get("tools");
            Assertions.assertFalse(tools.isEmpty());
            boolean testToolFound = false;
            boolean weatherToolFound = false;
            for(Map<String, Object> toolMap: tools) {
                if("testTool".equals(toolMap.get("name"))) testToolFound = true;
                if("weather".equals(toolMap.get("name"))) weatherToolFound = true;
            }
            Assertions.assertTrue(testToolFound);
            Assertions.assertTrue(weatherToolFound);

        } finally {


            client.restore(token);


        }
    }

    @Test
    public void testMcpProxy() throws Exception {
        // Register an MCP proxy tool
        McpTool proxyTool = new McpProxyTool("mcpTool", "Proxy to backend MCP", "mcpTool@call", "/mcp-backend", "POST", null, null, null, null, "http://localhost:7081");
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
            Assertions.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            Assertions.assertNotNull(body);
            // Verify result contains echo from backend
            Assertions.assertTrue(body.contains("echo from mcp backend"));

        } finally {


            client.restore(token);


        }
    }

    @Test
    public void testServiceIdWithoutClusterFailsGracefully() {
        HttpMcpTool tool = new HttpMcpTool("weatherByService", "Get weather information", "weatherByService@call", "/weather", "GET", null, "http", "weather-service", null, null);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> tool.execute(Map.of()));
        Assertions.assertTrue(exception.getMessage().contains("Cluster service is not available"));
    }

    @Test
    public void testMcpProxyServiceIdWithoutClusterFailsGracefully() {
        McpProxyTool tool = new McpProxyTool("mcpByService", "Proxy to backend MCP", "mcpByService@call", "/mcp-backend", "POST", null, "http", "mcp-service", null, null);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> tool.execute(Map.of()));
        Assertions.assertTrue(exception.getMessage().contains("Cluster service is not available"));
    }

    @Test
    public void testToolCallResponseFilterForTextContent() throws Exception {
        McpTool tool = new McpTool() {
            @Override
            public String getName() { return "textFilterTool"; }
            @Override
            public String getDescription() { return "A text filter tool"; }
            @Override
            public String getEndpoint() { return "/v1/accounts/123@get"; }
            @Override
            public String getInputSchema() { return "{\"type\": \"object\"}"; }
            @Override
            public Map<String, Object> execute(Map<String, Object> arguments) {
                return Map.of("content", List.of(Map.of("type", "text", "text", "[{\"accountNo\":123}]")));
            }
        };
        McpToolRegistry.registerTool(tool);
        testRuleExecutor.setEndpointRules(Map.of(
                "/v1/accounts@get",
                Map.of("res-fil", List.of("filterText"))));

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token = client.borrow(
                new URI("http://localhost:" + PORT),
                Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"textFilterTool\", \"arguments\": {}}, \"id\": 5}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assertions.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            Map<String, Object> map = Config.getInstance().getMapper().readValue(body, Map.class);
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            Assertions.assertEquals("[{\"filtered\":true}]", content.get(0).get("text"));
        } finally {
            client.restore(token);
            testRuleExecutor.setEndpointRules(new HashMap<>());
        }
    }

    @Test
    public void testToolCallResponseFilterForStructuredContent() throws Exception {
        McpTool tool = new McpTool() {
            @Override
            public String getName() { return "structuredFilterTool"; }
            @Override
            public String getDescription() { return "A structured filter tool"; }
            @Override
            public String getEndpoint() { return "/v1/pets@get"; }
            @Override
            public String getInputSchema() { return "{\"type\": \"object\"}"; }
            @Override
            public Map<String, Object> execute(Map<String, Object> arguments) {
                return Map.of("structuredContent", Map.of("id", 1, "name", "dog"));
            }
        };
        McpToolRegistry.registerTool(tool);
        testRuleExecutor.setEndpointRules(Map.of(
                "/v1/pets@get",
                Map.of("res-fil", List.of("filterStructured"))));

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token = client.borrow(
                new URI("http://localhost:" + PORT),
                Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            String json = "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"structuredFilterTool\", \"arguments\": {}}, \"id\": 6}";
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/mcp");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            request.getRequestHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(io.undertow.util.Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
            latch.await(1000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            Assertions.assertEquals(200, response.getResponseCode());
            String body = response.getAttachment(Http2Client.RESPONSE_BODY);
            Map<String, Object> map = Config.getInstance().getMapper().readValue(body, Map.class);
            Map<String, Object> result = (Map<String, Object>) map.get("result");
            Map<String, Object> structuredContent = (Map<String, Object>) result.get("structuredContent");
            Assertions.assertEquals(Boolean.TRUE, structuredContent.get("filtered"));
        } finally {
            client.restore(token);
            testRuleExecutor.setEndpointRules(new HashMap<>());
        }
    }

    static class TestRuleExecutor implements RuleExecutor {
        private Map<String, Object> endpointRules = new HashMap<>();

        @Override
        public Map<String, Object> executeRule(String ruleId, Map<String, Object> input) {
            String responseBody = (String) input.get("responseBody");
            Map<String, Object> result = new HashMap<>();
            result.put(RuleConstants.RESULT, true);
            if ("filterText".equals(ruleId)) {
                result.put("responseBody", "[{\"filtered\":true}]");
            } else if ("filterStructured".equals(ruleId)) {
                result.put("responseBody", "{\"filtered\":true}");
            } else {
                result.put("responseBody", responseBody);
            }
            return result;
        }

        @Override
        public Map<String, Object> executeRules(List<String> ruleIds, String logic, Map<String, Object> objMap) {
            return null;
        }

        @Override
        public Map<String, Object> executeRules(String serviceEntry, String ruleType, Map<String, Object> objMap) {
            Map<String, Object> result = new HashMap<>();
            result.put(RuleConstants.RESULT, true);
            return result;
        }

        @Override
        public RuleEngine getRuleEngine() {
            return null;
        }

        @Override
        public Map<String, Object> getEndpointRules() {
            return endpointRules;
        }

        @Override
        public void setEndpointRules(Map<String, Object> endpointRules) {
            this.endpointRules = endpointRules;
        }

        @Override
        public Map<String, Rule> getRules() {
            return null;
        }

        @Override
        public void setRules(Map<String, Rule> rules) {
        }
    }
}
