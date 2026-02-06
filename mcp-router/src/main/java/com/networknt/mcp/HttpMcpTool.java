package com.networknt.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An McpTool implementation that proxies requests to a backend HTTP service.
 *
 * @author Steve Hu
 */
public class HttpMcpTool implements McpTool {
    private static final Logger logger = LoggerFactory.getLogger(HttpMcpTool.class);
    private static final Http2Client client = Http2Client.getInstance();
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    private final String name;
    private final String description;
    private final String host;
    private final String path;
    private final String method;
    private final String inputSchema;

    public HttpMcpTool(String name, String description, String host, String path, String method, String inputSchema) {
        this.name = name;
        this.description = description;
        this.host = host;
        this.path = path;
        this.method = method;
        this.inputSchema = inputSchema;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getInputSchema() {
        if (inputSchema != null) {
            return inputSchema;
        }
        // For now, return a generic schema as we don't have validation info from config yet
        return "{\"type\": \"object\"}";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        ClientConnection connection = null;
        try {
            URI uri = new URI(host);
            connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            
            ClientRequest request = new ClientRequest().setMethod(Methods.fromString(method));
            
            if ("GET".equalsIgnoreCase(method)) {
                StringBuilder queryParams = new StringBuilder();
                if (arguments != null && !arguments.isEmpty()) {
                    queryParams.append("?");
                    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                        queryParams.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                    }
                    queryParams.setLength(queryParams.length() - 1); // Remove trailing &
                }
                request.setPath(path + queryParams.toString());
            } else {
                request.setPath(path);
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                // Body will be sent in callback
            }
            String hostHeader = uri.getHost();
            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) {
                hostHeader += ":" + port;
            }
            request.getRequestHeaders().put(Headers.HOST, hostHeader);

            if ("GET".equalsIgnoreCase(method)) {
                 connection.sendRequest(request, client.createClientCallback(reference, latch));
            } else {
                 String jsonBody = mapper.writeValueAsString(arguments);
                 if(logger.isDebugEnabled()) logger.debug("Transformed body: {}", jsonBody);
                 connection.sendRequest(request, client.createClientCallback(reference, latch, jsonBody));
            }

            latch.await(5000, TimeUnit.MILLISECONDS); // Default 5s timeout
            
            ClientResponse response = reference.get();
            if (response != null) {
                int statusCode = response.getResponseCode();
                String body = response.getAttachment(Http2Client.RESPONSE_BODY);
                
                if (statusCode >= 200 && statusCode < 300) {
                     if (body != null && !body.isEmpty()) {
                         try {
                             return mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                         } catch (Exception e) {
                             // If not JSON, return generic content wrapper? Or just error?
                             // Specification says result is text/image content.
                             // For flexibility, let's wrap non-JSON string in a simple map or try to return as map if possible.
                             // Example: { "content": [{ "type": "text", "text": body }] }
                             return Map.of("content", java.util.List.of(Map.of("type", "text", "text", body)));
                         }
                     }
                     return Map.of("result", "success");
                } else {
                    throw new RuntimeException("Backend service " + name + " failed with status " + statusCode + ": " + body);
                }
            } else {
                throw new RuntimeException("Timeout waiting for backend service " + name);
            }

        } catch (Exception e) {
            logger.error("Error executing HttpMcpTool " + name, e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage());
        } finally {
            IoUtils.safeClose(connection);
        }
    }
}
