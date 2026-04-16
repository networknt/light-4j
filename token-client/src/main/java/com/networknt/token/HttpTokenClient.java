package com.networknt.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import io.undertow.UndertowOptions;
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
 * L3 HTTP Client for Tokenization. Connects outward to the persistent Light-Tokenization Database Service.
 */
public class HttpTokenClient implements TokenClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpTokenClient.class);
    private static final ObjectMapper mapper = Config.getInstance().getMapper();
    private static final Http2Client client = Http2Client.getInstance();
    private static final int REQUEST_TIMEOUT_SECONDS = 3;

    private final String serviceUrl;

    public HttpTokenClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    private String executeRequest(URI uri, ClientRequest request, String requestBody, String operation) throws Exception {
        SimpleConnectionState.ConnectionToken token = null;
        try {
            token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
            ClientConnection connection = (ClientConnection) token.connection().getRawConnection();

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();

            if (requestBody != null) {
                connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            } else {
                connection.sendRequest(request, client.createClientCallback(reference, latch));
            }

            if (!latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.error("{} request timed out after {} seconds", operation, REQUEST_TIMEOUT_SECONDS);
                throw new IllegalStateException(operation + " failed with status: timeout");
            }

            ClientResponse response = reference.get();
            if (response != null && response.getResponseCode() == 200) {
                return response.getAttachment(Http2Client.RESPONSE_BODY);
            }

            String status = response != null ? String.valueOf(response.getResponseCode()) : "timeout";
            logger.error("{} failed with status: {}", operation, status);
            throw new IllegalStateException(operation + " failed with status: " + status);
        } finally {
            if (token != null) {
                client.restore(token);
            }
        }
    }

    @Override
    public String tokenize(String value, int schemeId) {
        if (value == null || value.isEmpty()) return value;

        try {
            URI uri = new URI(serviceUrl);
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/v1/token");
            String hostHeader = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
            request.getRequestHeaders().put(Headers.HOST, hostHeader);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("value", value);
            reqBody.put("schemeId", schemeId);
            String body = mapper.writeValueAsString(reqBody);

            return executeRequest(uri, request, body, "Tokenization");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Tokenization interrupted", e);
            throw new IllegalStateException("Tokenization interrupted", e);
        } catch (Exception e) {
            logger.error("Exception during tokenization", e);
            throw new IllegalStateException("Exception during tokenization", e);
        }
    }

    @Override
    public String detokenize(String tokenPrefix) {
        if (tokenPrefix == null || tokenPrefix.isEmpty()) return tokenPrefix;

        try {
            URI uri = new URI(serviceUrl);
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/v1/token/" + tokenPrefix);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            return executeRequest(uri, request, null, "Detokenization");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Detokenization interrupted", e);
            throw new IllegalStateException("Detokenization interrupted", e);
        } catch (Exception e) {
            logger.error("Exception during detokenization", e);
            throw new IllegalStateException("Exception during detokenization", e);
        }
    }
}
