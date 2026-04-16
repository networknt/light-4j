package com.networknt.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.access.AccessControlConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleExecutor;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ConfigUtils;
import com.networknt.utility.Constants;
import com.networknt.mask.Mask;
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
import com.networknt.token.TokenClient;
import com.networknt.token.CacheTokenClient;
import com.networknt.token.HttpTokenClient;

public class McpHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final String JSONRpc_VERSION = "2.0";
    private static final String REQUEST_ACCESS = "req-acc";
    private static final String RESPONSE_FILTER = "res-fil";
    private static final String PERMISSION = "permission";
    private static final String RESPONSE_BODY = "responseBody";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String STATUS_CODE = "statusCode";
    private static final String AUDIT_INFO = "auditInfo";
    private static final String ENDPOINT = "endpoint";
    private static final String TOOL_ARGUMENTS = "toolArguments";
    private static final String TOOL_RESULT = "toolResult";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String CODE = "code";

    // Initialize Token Framework locally (L1/L2 -> L3 HTTP Fallback)
    private static final TokenClient tokenClient = new CacheTokenClient(new HttpTokenClient("https://tokenization.lightapi.net"));

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
                String targetHost = toolData.getTargetHost();
                String serviceId = toolData.getServiceId();

                if (name != null && endpoint != null && ((serviceId != null && protocol != null) || targetHost != null)) {
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
                processMessage(exch, message);
            } catch (Exception e) {
                logger.error("Error processing MCP message", e);
                exch.setStatusCode(500);
                exch.getResponseSender().send("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32700, \"message\": \"Parse error\"}, \"id\": null}");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void processMessage(HttpServerExchange exch, String message) throws Exception {
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
            return;
        } else if ("tools/list".equals(method)) {
            addToolsListResponse(request, response);
        } else if ("tools/call".equals(method)) {
            if (handleToolCall(exch, request, response)) {
                return;
            }
        } else {
            response.put(ERROR, Map.of(CODE, -32601, MESSAGE, "Method not found: " + method));
        }

        exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exch.getResponseSender().send(mapper.writeValueAsString(response));
    }

    @SuppressWarnings("unchecked")
    private void addToolsListResponse(Map<String, Object> request, Map<String, Object> response) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String query = null;
        String intent = null;
        if (params != null) {
            query = params.get("query") != null ? params.get("query").toString().toLowerCase() : null;
            intent = params.get("intent") != null ? params.get("intent").toString().toLowerCase() : null;
        }

        List<Map<String, Object>> toolList = new ArrayList<>();
        for (McpTool tool : McpToolRegistry.getTools().values()) {
            if (!matchesToolFilter(tool, query, intent)) {
                continue;
            }

            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("name", tool.getName());
            toolMap.put("description", tool.getDescription());
            toolMap.put("inputSchema", parseInputSchema(tool));
            toolList.add(toolMap);
        }
        response.put("result", Map.of("tools", toolList));
    }

    private boolean matchesToolFilter(McpTool tool, String query, String intent) {
        boolean match = true;

        if (query != null) {
            match = (tool.getName() != null && tool.getName().toLowerCase().contains(query)) ||
                    (tool.getDescription() != null && tool.getDescription().toLowerCase().contains(query));
        }

        if (match && intent != null) {
            match = (tool.getName() != null && tool.getName().toLowerCase().contains(intent)) ||
                    (tool.getDescription() != null && tool.getDescription().toLowerCase().contains(intent));
        }

        return match;
    }

    private Object parseInputSchema(McpTool tool) {
        try {
            if(tool.getInputSchema() != null) {
                return mapper.readTree(tool.getInputSchema());
            }
        } catch (Exception e) {
            logger.warn("Invalid JSON schema for tool {}", tool.getName(), e);
        }
        return Map.of("type", "object");
    }

    @SuppressWarnings("unchecked")
    private boolean handleToolCall(HttpServerExchange exch, Map<String, Object> request, Map<String, Object> response) throws Exception {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.get("arguments");

        Map<String, Object> auditInfo = exch.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo == null) {
            auditInfo = new HashMap<>();
            exch.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
        }
        auditInfo.put("toolName", toolName);
        if (args != null) {
            putAuditJson(auditInfo, TOOL_ARGUMENTS, args);
        }

        McpTool tool = McpToolRegistry.getTool(toolName);
        if (tool == null) {
            Map<String, Object> errorMap = Map.of(CODE, -32601, MESSAGE, "Tool not found: " + toolName);
            response.put(ERROR, errorMap);
            putAuditJson(auditInfo, TOOL_RESULT, errorMap);
            return false;
        }

        if (!checkAccessControl(exch, tool.getEndpoint(), args, response)) {
            putAuditJson(auditInfo, TOOL_RESULT, response.get(ERROR));
            exch.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exch.getResponseSender().send(mapper.writeValueAsString(response));
            return true;
        }

        executeTool(exch, toolName, args, auditInfo, tool, response);
        return false;
    }

    @SuppressWarnings("unchecked")
    private void executeTool(HttpServerExchange exch, String toolName, Map<String, Object> args, Map<String, Object> auditInfo,
                             McpTool tool, Map<String, Object> response) {
        try {
            Map<String, Object> maskedArgs = maskToolArguments(toolName, args, tool);
            Map<String, Object> result = tool.execute(maskedArgs);
            result = applyResponseFilter(exch, tool.getEndpoint(), maskedArgs, result);
            if (result != null) {
                result = maskToolResult(toolName, result);
            }

            response.put("result", result);
            if (result != null) {
                putAuditJson(auditInfo, TOOL_RESULT, result);
            }
        } catch (Exception e) {
            logger.error("Tool execution error", e);
            Map<String, Object> errorMap = Map.of(CODE, -32000, MESSAGE, "Tool execution failed: " + e.getMessage());
            response.put(ERROR, errorMap);
            putAuditJson(auditInfo, TOOL_RESULT, errorMap);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> maskToolArguments(String toolName, Map<String, Object> args, McpTool tool) throws Exception {
        Map<String, String> requestRules = McpMaskingUtils.getMaskingRulesFromSchema(toolName, tool.getInputSchema());
        if (requestRules != null && !requestRules.isEmpty()) {
            String argsStr = mapper.writeValueAsString(args);
            String maskedArgsStr = Mask.maskJson(argsStr, requestRules);
            args = mapper.readValue(maskedArgsStr, Map.class);
        }

        Map<String, Integer> tokenizeRules = McpMaskingUtils.getTokenizationRulesFromSchema(toolName, tool.getInputSchema());
        if (tokenizeRules != null && !tokenizeRules.isEmpty()) {
            args = tokenizeArguments(args, tokenizeRules);
        }

        String globalArgsStr = mapper.writeValueAsString(args);
        String finalArgsStr = Mask.maskString(globalArgsStr, "mcp_global");
        if (!globalArgsStr.equals(finalArgsStr)) {
            args = mapper.readValue(finalArgsStr, Map.class);
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> maskToolResult(String toolName, Map<String, Object> result) throws Exception {
        String resultStr = mapper.writeValueAsString(result);
        String maskedResultStr = Mask.maskJson(resultStr, toolName);
        maskedResultStr = Mask.maskString(maskedResultStr, "mcp_global");
        return mapper.readValue(maskedResultStr, Map.class);
    }

    private void putAuditJson(Map<String, Object> auditInfo, String key, Object value) {
        try {
            auditInfo.put(key, mapper.writeValueAsString(value));
        } catch (Exception e) {
            auditInfo.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tokenizeArguments(Map<String, Object> args, Map<String, Integer> tokenizeRules) throws Exception {
        com.jayway.jsonpath.DocumentContext ctx = com.jayway.jsonpath.JsonPath.parse(mapper.writeValueAsString(args));
        com.jayway.jsonpath.Configuration conf = com.jayway.jsonpath.Configuration.builder().options(com.jayway.jsonpath.Option.AS_PATH_LIST).build();
        com.jayway.jsonpath.DocumentContext pathCtx = com.jayway.jsonpath.JsonPath.using(conf).parse(ctx.jsonString());

        for (Map.Entry<String, Integer> entry : tokenizeRules.entrySet()) {
            tokenizePath(ctx, pathCtx, entry.getKey(), entry.getValue());
        }
        return mapper.readValue(ctx.jsonString(), Map.class);
    }

    private void tokenizePath(com.jayway.jsonpath.DocumentContext ctx,
                              com.jayway.jsonpath.DocumentContext pathCtx,
                              String abstractPath,
                              Integer schemeId) {
        try {
            List<String> actualPaths = pathCtx.read(abstractPath);
            for (String actualPath : actualPaths) {
                Object val = ctx.read(actualPath);
                if (val instanceof String string) {
                    ctx.set(actualPath, tokenClient.tokenize(string, schemeId));
                }
            }
        } catch (com.jayway.jsonpath.PathNotFoundException e) {
            // Ignore if path not found in payload
        }
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
                response.put(ERROR, Map.of(CODE, -32001, MESSAGE, "Access denied: no access control rule defined for " + endpoint));
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
                response.put(ERROR, Map.of(CODE, -32001, MESSAGE, "Access denied by access control rule for " + endpoint));
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyResponseFilter(HttpServerExchange exchange, String endpoint, Map<String, Object> args, Map<String, Object> result) {
        if (ruleExecutor == null || endpoint == null || result == null) {
            return result;
        }

        Map<String, Object> endpointRules = ruleExecutor.getEndpointRules();
        if (endpointRules == null || endpointRules.isEmpty()) {
            return result;
        }

        String endpointPath = endpoint;
        String endpointMethod = "call";
        int atIndex = endpoint.indexOf('@');
        if (atIndex >= 0) {
            endpointPath = endpoint.substring(0, atIndex);
            if (atIndex < endpoint.length() - 1) {
                endpointMethod = endpoint.substring(atIndex + 1);
            }
        }

        String serviceEntry = endpointRules.containsKey(endpoint)
                ? endpoint
                : ConfigUtils.findServiceEntry(endpointMethod, endpointPath, endpointRules);
        if (serviceEntry == null) {
            return result;
        }

        Map<String, List> serviceEntryRules = (Map<String, List>) endpointRules.get(serviceEntry);
        if (serviceEntryRules == null) {
            return result;
        }

        List<String> responseRules = serviceEntryRules.get(RESPONSE_FILTER);
        if (responseRules == null || responseRules.isEmpty()) {
            return result;
        }

        FilterTarget filterTarget = FilterTarget.from(result);
        if (filterTarget == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Skipping MCP response filter for endpoint {} because the result is not filterable. result={}",
                        endpoint, JsonMapper.toJson(result));
            }
            return result;
        }

        String responseBody = filterTarget.responseBody();
        Map<String, Object> objMap = new HashMap<>();
        objMap.put(RESPONSE_BODY, responseBody);
        objMap.put(AUDIT_INFO, exchange.getAttachment(AttachmentConstants.AUDIT_INFO));
        objMap.put(STATUS_CODE, exchange.getStatusCode());
        objMap.put(ENDPOINT, endpoint);
        objMap.put(TOOL_ARGUMENTS, args);

        boolean finalResult = true;
        Map<String, Object> ruleResult = null;
        for (String ruleId : responseRules) {
            Map<String, Object> permissionMap = (Map<String, Object>) serviceEntryRules.get(PERMISSION);
            if (permissionMap != null) {
                objMap.put(Constants.COL, permissionMap.get(Constants.COL));
                objMap.put(Constants.ROW, permissionMap.get(Constants.ROW));
            }
            ruleResult = ruleExecutor.executeRule(ruleId, objMap);
            boolean res = Boolean.TRUE.equals(ruleResult == null ? null : ruleResult.get(RuleConstants.RESULT));
            if (!res) {
                finalResult = false;
                break;
            }
            responseBody = (String) ruleResult.get(RESPONSE_BODY);
            objMap.put(RESPONSE_BODY, responseBody);
        }

        if (!finalResult) {
            if (logger.isTraceEnabled()) {
                logger.trace("MCP response filter condition not met for endpoint {}: {}", endpoint,
                        ruleResult == null ? null : ruleResult.get(ERROR_MESSAGE));
            }
            return result;
        }

        if (ruleResult == null) {
            return result;
        }

        responseBody = (String) ruleResult.get(RESPONSE_BODY);
        if (responseBody == null) {
            return result;
        }

        try {
            return filterTarget.applyFiltered(responseBody, mapper);
        } catch (Exception e) {
            logger.warn("Unable to apply filtered MCP response for endpoint {}. Returning original result.", endpoint, e);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static final class FilterTarget {
        private final Map<String, Object> result;
        private final Object structuredContent;
        private final Map<String, Object> contentItem;
        private final String responseBody;

        private FilterTarget(Map<String, Object> result, Object structuredContent, Map<String, Object> contentItem, String responseBody) {
            this.result = result;
            this.structuredContent = structuredContent;
            this.contentItem = contentItem;
            this.responseBody = responseBody;
        }

        private static FilterTarget from(Map<String, Object> result) {
            Object structuredContent = result.get("structuredContent");
            if (structuredContent != null) {
                return new FilterTarget(result, structuredContent, null, JsonMapper.toJson(structuredContent));
            }

            Object contentObject = result.get("content");
            if (!(contentObject instanceof List<?> contentList) || contentList.size() != 1) {
                return null;
            }

            Object itemObject = contentList.get(0);
            if (!(itemObject instanceof Map<?, ?> rawContentItem)) {
                return null;
            }

            Object type = rawContentItem.get("type");
            Object text = rawContentItem.get("text");
            if (!"text".equals(type) || !(text instanceof String textValue)) {
                return null;
            }

            return new FilterTarget(result, null, (Map<String, Object>) rawContentItem, textValue);
        }

        private String responseBody() {
            return responseBody;
        }

        private Map<String, Object> applyFiltered(String filteredBody, ObjectMapper mapper) throws Exception {
            if (structuredContent != null) {
                Map<String, Object> newResult = new HashMap<>(result);
                newResult.put("structuredContent", mapper.readValue(filteredBody, Object.class));
                return newResult;
            }

            if (contentItem != null) {
                Map<String, Object> newResult = new HashMap<>(result);
                Map<String, Object> newContentItem = new HashMap<>(contentItem);
                newContentItem.put("text", filteredBody);
                newResult.put("content", List.of(newContentItem));
                return newResult;
            }

            return result;
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
