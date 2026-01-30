package com.networknt.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.server.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import com.networknt.sse.SseConnectionRegistry;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * McpHandler is the main handler for the MCP Router.
 *
 * @author Steve Hu
 */
public class McpHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final String JSONRpc_VERSION = "2.0";

    private volatile HttpHandler next;
    private McpConfig config;
    private ObjectMapper mapper = Config.getInstance().getMapper();

    /**
     * Default constructor
     */
    public McpHandler() {
        McpConfig config = McpConfig.load();
        List<Map<String, Object>> tools = config.getTools();
        if (tools != null) {
            for (Map<String, Object> toolData : tools) {
                String name = (String) toolData.get("name");
                String description = (String) toolData.get("description");
                String host = (String) toolData.get("host");
                String path = (String) toolData.get("path");
                String method = (String) toolData.get("method");
                if (name != null && host != null && path != null && method != null) {
                    McpToolRegistry.registerTool(new HttpMcpTool(name, description, host, path, method));
                    if (logger.isDebugEnabled()) logger.debug("Registered MCP tool: {}", name);
                } else {
                    logger.error("Invalid tool configuration: {}", toolData);
                }
            }
        }
        if(logger.isInfoEnabled()) logger.info("McpHandler initialized.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        McpConfig config = McpConfig.load();
        String path = exchange.getRequestPath();

        if (config.getSsePath().equals(path) && exchange.getRequestMethod().equals(Methods.GET)) {
            handleSse(exchange, config);
        } else if (config.getMessagePath().equals(path) && exchange.getRequestMethod().equals(Methods.POST)) {
            handleMessage(exchange, config);
        } else {
            Handler.next(exchange, next);
        }
    }

    private void handleSse(HttpServerExchange exchange, McpConfig config) throws Exception {
        ServerSentEventHandler sseHandler = new ServerSentEventHandler((connection, lastEventId) -> {
            String id = UUID.randomUUID().toString();
            // In a real scenario, you might want check headers for existing ID or token.
            SseConnectionRegistry.add(id, connection);
            if(logger.isDebugEnabled()) logger.debug("New MCP SSE connection established: {}", id);
            
            // Send the endpoint event as per MCP HTTP transport spec
            try {
                // The data field contains the URI for the POST endpoint (messagePath)
                connection.send(config.getMessagePath(), "endpoint", null, null);
            } catch (Exception e) {
                logger.error("Failed to send endpoint event", e);
            }

            connection.addCloseTask(channel -> {
                if(logger.isDebugEnabled()) logger.debug("MCP SSE connection closed: {}", id);
                SseConnectionRegistry.remove(id);
            });
        });
        sseHandler.handleRequest(exchange);
    }

    private void handleMessage(HttpServerExchange exchange, McpConfig config) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        
        exchange.getRequestReceiver().receiveFullString((exch, message) -> {
            try {
                Map<String, Object> request = mapper.readValue(message, Map.class);
                String method = (String) request.get("method");
                Object id = request.get("id");

                Map<String, Object> response = new HashMap<>();
                response.put("jsonrpc", JSONRpc_VERSION);
                response.put("id", id);

                if ("initialize".equals(method)) {
                    Map<String, Object> textResult = new HashMap<>();
                    textResult.put("protocolVersion", "2024-11-05");
                    textResult.put("capabilities", Map.of("tools", Map.of("listChanged", true)));
                    textResult.put("serverInfo", Map.of("name", "light-4j-mcp", "version", "1.0.0"));
                    response.put("result", textResult);
                } else if ("notifications/initialized".equals(method)) {
                    // client ready, no response needed for notification
                    return;
                } else if ("tools/list".equals(method)) {
                    List<Map<String, Object>> toolList = new ArrayList<>();
                    for (McpTool tool : McpToolRegistry.getTools().values()) {
                        Map<String, Object> t = new HashMap<>();
                        t.put("name", tool.getName());
                        t.put("description", tool.getDescription());
                        try {
                            // Parse schema string to JSON structure
                            if(tool.getInputSchema() != null) {
                                t.put("inputSchema", mapper.readTree(tool.getInputSchema()));
                            } else {
                                t.put("inputSchema", Map.of("type", "object")); 
                            }
                        } catch (Exception e) {
                             logger.warn("Invalid JSON schema for tool {}", tool.getName(), e);
                             t.put("inputSchema", Map.of("type", "object"));
                        }
                        toolList.add(t);
                    }
                    response.put("result", Map.of("tools", toolList));
                } else if ("tools/call".equals(method)) {
                    Map<String, Object> params = (Map<String, Object>) request.get("params");
                    String toolName = (String) params.get("name");
                    Map<String, Object> args = (Map<String, Object>) params.get("arguments");

                    McpTool tool = McpToolRegistry.getTool(toolName);
                    if (tool != null) {
                        try {
                            Map<String, Object> result = tool.execute(args);
                            response.put("result", result);
                        } catch (Exception e) {
                             logger.error("Tool execution error", e);
                             response.put("error", Map.of("code", -32000, "message", "Tool execution failed: " + e.getMessage()));
                        }
                    } else {
                        response.put("error", Map.of("code", -32601, "message", "Tool not found: " + toolName));
                    }
                } else {
                    response.put("error", Map.of("code", -32601, "message", "Method not found: " + method));
                }

                exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exch.getResponseSender().send(mapper.writeValueAsString(response));

            } catch (Exception e) {
                logger.error("Error processing MCP message", e);
                exch.setStatusCode(500);
                exch.getResponseSender().send("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32700, \"message\": \"Parse error\"}, \"id\": null}");
            }
        });
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
        return McpConfig.load().isEnabled();
    }

}
