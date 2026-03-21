package com.networknt.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * McpProxyTool routes MCP tool calls to a backend MCP server.
 * It wraps the arguments in a JSON-RPC "tools/call" request.
 */
public class McpProxyTool extends AbstractRemoteMcpTool {
    private static final Logger logger = LoggerFactory.getLogger(McpProxyTool.class);
    private static final String JSONRpc_VERSION = "2.0";
    private final ObjectMapper mapper = Config.getInstance().getMapper();

    public McpProxyTool(String name, String description, String endpoint, String path, String method, String inputSchema, String protocol, String serviceId, String envTag, String targetHost) {
        super(name, description, endpoint, path, method, inputSchema, protocol, serviceId, envTag, targetHost);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        // Construct JSON-RPC request for the backend
        Map<String, Object> jsonRpcRequest = new HashMap<>();
        jsonRpcRequest.put("jsonrpc", JSONRpc_VERSION);
        jsonRpcRequest.put("method", "tools/call");
        jsonRpcRequest.put("id", System.currentTimeMillis()); // generate a request ID
        Map<String, Object> params = new HashMap<>();
        params.put("name", this.name);
        params.put("arguments", arguments);
        jsonRpcRequest.put("params", params);

        Http2Client client = Http2Client.getInstance();
        SimpleConnectionState.ConnectionToken token = null;
        ClientConnection connection = null;
        try {
            String url = resolveTargetUrl();
            URI uri = new URI(url);
            token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            connection = (ClientConnection) token.getRawConnection();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();

            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(path);
            request.getRequestHeaders().put(Headers.HOST, buildHostHeader(uri));
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");

            String jsonBody = mapper.writeValueAsString(jsonRpcRequest);
            if(logger.isDebugEnabled()) logger.debug("Backend MCP request: {}", jsonBody);

            connection.sendRequest(request, client.createClientCallback(reference, latch, jsonBody));
            latch.await(3000, TimeUnit.MILLISECONDS);

            ClientResponse response = reference.get();
            if (response != null) {
                int statusCode = response.getResponseCode();
                String responseBody = response.getAttachment(Http2Client.RESPONSE_BODY);

                if(logger.isDebugEnabled()) logger.debug("Backend MCP response: {}", responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    // Parse backend JSON-RPC response
                    Map<String, Object> jsonRpcResponse = mapper.readValue(responseBody, Map.class);
                    if (jsonRpcResponse.containsKey("error")) {
                       Map<String, Object> error = (Map<String, Object>) jsonRpcResponse.get("error");
                       throw new RuntimeException("Backend MCP error: " + error.get("message"));
                    }
                    // extract result
                    Map<String, Object> result = (Map<String, Object>) jsonRpcResponse.get("result");
                    return result;
                } else {
                    throw new RuntimeException("Backend service " + name + " failed with status " + statusCode + ": " + responseBody);
                }
            } else {
                throw new RuntimeException("Timeout waiting for backend service " + name);
            }
        } catch (Exception e) {
            logger.error("Error executing McpProxyTool", e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
        } finally {
            client.restore(token);
        }
    }
}
