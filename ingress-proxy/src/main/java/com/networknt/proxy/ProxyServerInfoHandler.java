package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.info.ServerInfoGetHandler;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ProxyServerInfoHandler implements LightHttpHandler {
    static final String CONFIG_NAME = "proxy";
    static ProxyConfig config = (ProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);
    private static Http2Client client = Http2Client.getInstance();
    private static final int UNUSUAL_STATUS_CODE = 300;
    private static OptionMap optionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static final String PROXY_INFO_KEY = "proxy_info";

    public ProxyServerInfoHandler() {
        config = (ProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> proxyInfo = ServerInfoGetHandler.getServerInfo(exchange);
        result.put(PROXY_INFO_KEY, proxyInfo);
        HeaderValues authVal = exchange.getRequestHeaders().get(Headers.AUTHORIZATION_STRING);
        String token = authVal == null ? "" : authVal.get(0);
        List<String> urls = Arrays.asList(config.getHosts().split(","));
        for (String url : urls) {
            Map<String, Object> serverInfo;
            try {
                String serverInfoStr = getServerInfo(url, token);
                serverInfo = Config.getInstance().getMapper().readValue(serverInfoStr, Map.class);
            } catch (Exception e) {
                logger.error("cannot get server info for " + url, e);
                serverInfo = null;
            }
            result.put(url, serverInfo);
        }
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(result));

    }

    /**
     * get server info from url with token
     * @param url the url of the target server
     * @param token auth token
     * @return server info JSON string
     */
    public static String getServerInfo(String url, String token) {

        String res = "{}";
        ClientConnection connection = null;
        try {
            URI uri = new URI(url);
            if (config == null || !config.isHttpsEnabled()) {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            } else {
                connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
            }

            AtomicReference<ClientResponse> reference = send(connection, Methods.GET, "/server/info", token, null);
            if(reference != null && reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Server Info error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                    throw new RuntimeException();
                } else {
                    res = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                }
            }
        } catch (Exception e) {
            logger.error("Server info request exception", e);
            throw new RuntimeException("exception when getting server info", e);
        } finally {
            client.returnConnection(connection);
        }
        return res;
    }

    /**
     * send to service from controller with the health check and server info
     *
     * @param connection ClientConnection
     * @param path       path to send to controller
     * @param token      token to put in header
     * @return AtomicReference<ClientResponse> response
     */
    private static AtomicReference<ClientResponse> send(ClientConnection connection, HttpString method, String path, String token, String json) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
        // add host header for HTTP/1.1 server when HTTP is used.
        request.getRequestHeaders().put(Headers.HOST, "localhost");
        if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer "  + token);
        if(StringUtils.isBlank(json)) {
            connection.sendRequest(request, client.createClientCallback(reference, latch));
        } else {
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
        }
        latch.await(1000, TimeUnit.MILLISECONDS);
        return reference;
    }
}
