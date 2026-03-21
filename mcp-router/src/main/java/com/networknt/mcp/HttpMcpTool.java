package com.networknt.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An McpTool implementation that proxies requests to a backend HTTP service.
 *
 * @author Steve Hu
 */
public class HttpMcpTool extends AbstractRemoteMcpTool {
    private static final Logger logger = LoggerFactory.getLogger(HttpMcpTool.class);
    private static final Http2Client client = Http2Client.getInstance();
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    public HttpMcpTool(String name, String description, String endpoint, String path, String method, String inputSchema, String protocol, String serviceId, String envTag, String targetHost) {
        super(name, description, endpoint, path, method, inputSchema, protocol, serviceId, envTag, targetHost);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        SimpleConnectionState.ConnectionToken token = null;
        ClientConnection connection = null;
        try {
            String url = resolveTargetUrl();
            URI uri = new URI(url);
            token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            connection = (ClientConnection) token.getRawConnection();

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
            request.getRequestHeaders().put(Headers.HOST, buildHostHeader(uri));

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
            client.restore(token);
        }
    }
}
