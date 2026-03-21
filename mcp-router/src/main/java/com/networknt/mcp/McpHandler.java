package com.networknt.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.access.AccessControlConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleExecutor;
import com.networknt.server.ModuleRegistry;
import com.networknt.service.SingletonServiceFactory;
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
    private static final String REQUEST_ACCESS = "req-acc";

    private volatile HttpHandler next;
    private volatile McpConfig config;
    private ObjectMapper mapper = Config.getInstance().getMapper();
    private final RuleExecutor ruleExecutor;

    /**
     * Default constructor
     */
    public McpHandler() {
        config = McpConfig.load();
        refreshTools(config);
        RuleExecutor re = null;
        try {
            re = SingletonServiceFactory.getBean(RuleExecutor.class);
        } catch (Exception e) {
            logger.info("RuleExecutor is not available, access control for MCP tools is disabled.");
        }
        this.ruleExecutor = re;
        if(logger.isInfoEnabled()) logger.info("McpHandler initialized.");
    }

    private void refreshTools(McpConfig config) {
        List<Tool> tools = config.getTools();
        if (tools != null) {
            // clear before adding in case this is reload.
            McpToolRegistry.clear();
            for (Tool toolData : tools) {
                String name = toolData.getName();
                String description = toolData.getDescription();
                String endpoint = toolData.getEndpoint();
                String path = toolData.getPath();
                String method = toolData.getMethod();
                String inputSchema = toolData.getInputSchema();
                String protocol = toolData.getProtocol();
                String apiType = toolData.getApiType();

                if (name != null && (endpoint != null || toolData.getServiceId() != null) && path != null && method != null) {
                    McpTool tool;
                    if ("mcp".equalsIgnoreCase(apiType)) {
                        tool = new McpProxyTool(name, description, endpoint, path, method, inputSchema, protocol, toolData.getServiceId(), toolData.getEnvTag(), toolData.getTargetHost());
                    } else {
                        tool = new HttpMcpTool(name, description, endpoint, path, method, inputSchema, protocol, toolData.getServiceId(), toolData.getEnvTag(), toolData.getTargetHost());
                    }
                    McpToolRegistry.registerTool(tool);
                    if (logger.isDebugEnabled()) logger.debug("Registered MCP tool: {}", name);
                } else {
                    logger.error("Invalid tool configuration: {}", toolData);
                }
            }
        }
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        McpConfig newConfig = McpConfig.load();
        if (newConfig != config) {
            synchronized (this) {
                if (newConfig != config) {
                    config = newConfig;
                    refreshTools(config);
                }
            }
        }

        String path = exchange.getRequestPath();

        if (config.getPath().equals(path)) {
            if (exchange.getRequestMethod().equals(Methods.GET)) {
                handleSse(exchange, config);
            } else if (exchange.getRequestMethod().equals(Methods.POST)) {
                handleMessage(exchange, config);
            } else {
                exchange.setStatusCode(405);
                exchange.getResponseSender().send("Method Not Allowed");
            }
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
                // We append sessionId to the path so that POST requests can identify the session
                String endpoint = config.getPath() + "?sessionId=" + id;
                connection.send(endpoint, "endpoint", null, null);
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

        // Extract session ID from query parameter
        String sessionId = null;
        Deque<String> sessionIdDeque = exchange.getQueryParameters().get("sessionId");
        if (sessionIdDeque != null && !sessionIdDeque.isEmpty()) {
            sessionId = sessionIdDeque.getFirst();
        }

        // Optionally valid sessionId against SseConnectionRegistry if needed for strict session checking
        // if (sessionId != null && !SseConnectionRegistry.contains(sessionId)) { ... }

        exchange.getRequestReceiver().receiveFullString((exch, message) -> {
            if(logger.isDebugEnabled()) logger.debug("JSON-RPC message: {}", message);
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
                        // Fine-grained access control check before tool execution
                        if (!checkAccessControl(exch, tool.getEndpoint(), args, response)) {
                            // Access denied, response already populated with error
                            exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exch.getResponseSender().send(mapper.writeValueAsString(response));
                            return;
                        }
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

    /**
     * Check fine-grained access control for a tool call using the RuleExecutor.
     * Returns true if access is allowed, false if denied.
     * When denied, the response map is populated with a JSON-RPC error.
     *
     * @param exchange the current exchange
     * @param endpoint the endpoint of the tool being called
     * @param args the tool arguments
     * @param response the JSON-RPC response map to populate on denial
     * @return true if access is allowed, false if denied
     */
    @SuppressWarnings("unchecked")
    private boolean checkAccessControl(HttpServerExchange exchange, String endpoint, Map<String, Object> args, Map<String, Object> response) {
        AccessControlConfig accessControlConfig;
        try {
            accessControlConfig = AccessControlConfig.load();
        } catch (Exception e) {
            // access-control.yml not present or not parseable — skip access control
            if(logger.isTraceEnabled()) logger.trace("AccessControlConfig not available, skipping access control for endpoint {}", endpoint);
            return true;
        }
        if (!accessControlConfig.isEnabled()) {
            if(logger.isTraceEnabled()) logger.trace("Access control is disabled, skipping for endpoint {}", endpoint);
            return true;
        }
        if (ruleExecutor == null) {
            if(logger.isTraceEnabled()) logger.trace("RuleExecutor not available, skipping access control for endpoint {}", endpoint);
            return true;
        }

        // Check skipPathPrefixes against toolName
        if (accessControlConfig.getSkipPathPrefixes() != null &&
                accessControlConfig.getSkipPathPrefixes().stream().anyMatch(endpoint::startsWith)) {
            if(logger.isTraceEnabled()) logger.trace("Skipping access control for tool {} based on skipPathPrefixes", endpoint);
            return true;
        }

        // Build the rule engine payload
        Map<String, Object> ruleEnginePayload = new HashMap<>();
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo != null) {
            ruleEnginePayload.put("auditInfo", auditInfo);
        }
        ruleEnginePayload.put("headers", exchange.getRequestHeaders());
        ruleEnginePayload.put("endpoint", endpoint);
        ruleEnginePayload.put("toolArguments", args);

        // Execute rules
        Map<String, Object> result = ruleExecutor.executeRules(endpoint, REQUEST_ACCESS, ruleEnginePayload);
        if (result == null) {
            if (accessControlConfig.isDefaultDeny()) {
                logger.error("Access control rule is missing and default deny is true for tool endpoint {}", endpoint);
                response.put("error", Map.of("code", -32001, "message", "Access denied: no access control rule defined for " + endpoint));
                return false;
            } else {
                // No rules defined and default allow — proceed
                return true;
            }
        } else {
            boolean res = (Boolean) result.getOrDefault(RuleConstants.RESULT, false);
            if (res) {
                return true;
            } else {
                logger.error("Access denied for tool endpoint {}: {}", endpoint, JsonMapper.toJson(result));
                response.put("error", Map.of("code", -32001, "message", "Access denied by access control rule for " + endpoint));
                return false;
            }
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
        return McpConfig.load().isEnabled();
    }

}
