package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.health.HealthConfig;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The health check for the http-sidecar to optionally invoke the backend API with proper connection
 * cache.
 *
 * @author Steve Hu
 */
public class ProxyHealthGetHandler implements LightHttpHandler {
    public static final String HEALTH_RESULT_OK = "OK";
    public static final String HEALTH_RESULT_ERROR = "ERROR";
    static final Logger logger = LoggerFactory.getLogger(ProxyHealthGetHandler.class);
    static final HealthConfig config = (HealthConfig) Config.getInstance().getJsonObjectConfig(HealthConfig.CONFIG_NAME, HealthConfig.class);
    static final Http2Client client = Http2Client.getInstance();
    // cached connection to the backend API to speed up the downstream check.
    static ClientConnection connection = null;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ProxyHealthGetHandler.handleRequest starts.");
        String result = HEALTH_RESULT_OK;
        // if backend is not connected, then error. Check the configuration to see if it is enabled.
        if(config.isDownstreamEnabled()) {
            result = backendHealth();
        }
        // for security reason, we don't output the details about the error. Users can check the log for the failure.
        if(HEALTH_RESULT_ERROR == result) {
            exchange.setStatusCode(400);
            if(logger.isDebugEnabled()) logger.debug("ProxyHealthGetHandler.handleRequest ends with an error.");
            exchange.getResponseSender().send(HEALTH_RESULT_ERROR);
        } else {
            exchange.setStatusCode(200);
            if(logger.isDebugEnabled()) logger.debug("ProxyHealthGetHandler.handleRequest ends.");
            exchange.getResponseSender().send(HEALTH_RESULT_OK);
        }
    }

    /**
     * Try to access the configurable /health endpoint on the backend API. return OK if a success response is returned.
     * Otherwise, ERROR is returned.
     *
     * @return result String of OK or ERROR.
     */
    private String backendHealth() {
        String result = HEALTH_RESULT_OK;
        long start = System.currentTimeMillis();
        if(connection == null || !connection.isOpen()) {
            try {
                if(config.getDownstreamHost().startsWith("https")) {
                    connection = client.borrowConnection(new URI(config.getDownstreamHost()), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
                } else {
                    connection = client.borrowConnection(new URI(config.getDownstreamHost()), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
                }
            } catch (Exception ex) {
                logger.error("Could not create connection to the backend:", ex);
                result = HEALTH_RESULT_ERROR;
                // if connection cannot be established, return error. The backend is not started yet.
                return result;
            }
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(config.getDownstreamPath());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(logger.isDebugEnabled()) logger.debug("statusCode = " + statusCode + " body  = " + body);
            if(statusCode >= 400) {
                // something happens on the backend and the health check is not respond.
                logger.error("Error due to error response from backend with status code = " + statusCode + " body = " + body);
                result = HEALTH_RESULT_ERROR;
            }
        } catch (Exception exception) {
            logger.error("Error while sending a health check request to the backend with exception: ", exception);
            // for Java EE backend like spring boot, the connection created and opened but might not ready. So we need to close
            // the connection if there are any exception here to work around the spring boot backend.
            if(connection != null && connection.isOpen()) {
                try { connection.close(); } catch (Exception e) { logger.error("Exception:", e); }
            }
            result = HEALTH_RESULT_ERROR;
        }
        long responseTime = System.currentTimeMillis() - start;
        if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
        return result;
    }
}
