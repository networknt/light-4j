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

    private final String serviceUrl;

    public HttpTokenClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public String tokenize(String value, int schemeId) {
        if (value == null || value.isEmpty()) return value;

        try {
            URI uri = new URI(serviceUrl);
            SimpleConnectionState.ConnectionToken token = null;
            try {
                token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
                ClientConnection connection = (ClientConnection) token.connection().getRawConnection();

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<ClientResponse> reference = new AtomicReference<>();

                ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/v1/token");
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");

                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("value", value);
                reqBody.put("schemeId", schemeId);
                String body = mapper.writeValueAsString(reqBody);

                connection.sendRequest(request, client.createClientCallback(reference, latch, body));

                latch.await(3, TimeUnit.SECONDS);

                ClientResponse response = reference.get();
                if (response != null && response.getResponseCode() == 200) {
                    return response.getAttachment(Http2Client.RESPONSE_BODY);
                } else {
                    logger.error("Tokenization failed with status: " + (response != null ? response.getResponseCode() : "timeout"));
                }
            } finally {
                if (token != null) {
                    client.restore(token);
                }
            }
        } catch (Exception e) {
            logger.error("Exception during tokenization", e);
        }
        return value;
    }

    @Override
    public String detokenize(String tokenPrefix) {
        // Implementation for detokenization HTTP GET -> /v1/token/{token}
        if (tokenPrefix == null || tokenPrefix.isEmpty()) return tokenPrefix;

        try {
            URI uri = new URI(serviceUrl);
            SimpleConnectionState.ConnectionToken token = null;
            try {
                token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
                ClientConnection connection = (ClientConnection) token.connection().getRawConnection();

                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<ClientResponse> reference = new AtomicReference<>();

                ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/v1/token/" + tokenPrefix);
                request.getRequestHeaders().put(Headers.HOST, "localhost");

                connection.sendRequest(request, client.createClientCallback(reference, latch, ""));

                latch.await(3, TimeUnit.SECONDS);

                ClientResponse response = reference.get();
                if (response != null && response.getResponseCode() == 200) {
                    return response.getAttachment(Http2Client.RESPONSE_BODY);
                } else {
                    logger.error("Detokenization failed with status: " + (response != null ? response.getResponseCode() : "timeout"));
                }
            } finally {
                if (token != null) {
                    client.restore(token);
                }
            }
        } catch (Exception e) {
            logger.error("Exception during detokenization", e);
        }
        return tokenPrefix;
    }
}
