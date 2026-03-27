package com.networknt.portal.registry.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.portal.registry.PortalRegistryConfig;
import com.networknt.portal.registry.PortalRegistryService;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import com.networknt.client.simplepool.SimpleConnectionState;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.networknt.portal.registry.PortalRegistryConfig.CONFIG_NAME;

public class PortalRegistryClientImpl implements PortalRegistryClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistryClientImpl.class);
    private static final int UNUSUAL_STATUS_CODE = 300;
    private Http2Client client = Http2Client.getInstance();

    private OptionMap optionMap;
    private URI uri;
    private URI wsUri;
    private volatile PortalRegistryWebSocketClient webSocketClient;
    private volatile Consumer<Map<String, Object>> notificationHandler;
    private volatile String currentToken;
    private final Map<String, PortalRegistryService> registeredServices = new ConcurrentHashMap<>();
    private final Map<String, SubscriptionState> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "portal-registry-ws-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private volatile boolean replayInProgress;

    /**
     * Construct PortalRegistryClient with all parameters from portal-registry.yml config file. The other two constructors are
     * just for backward compatibility.
     */
    public PortalRegistryClientImpl() {
        PortalRegistryConfig config = PortalRegistryConfig.load();
        String portalUrl = config.getPortalUrl().toLowerCase();
        optionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
        logger.debug("url = {}", portalUrl);
        try {
            uri = new URI(portalUrl);
            wsUri = new URI(toWebSocketUrl(portalUrl) + "/ws");
        } catch (URISyntaxException e) {
            logger.error("Invalid URI " + portalUrl, e);
            throw new RuntimeException("Invalid URI " + portalUrl, e);
        }
    }

    @Override
    public void checkPass(PortalRegistryService service, String token) {
        String key = service.getTag() == null ? service.getServiceId() : service.getServiceId() + "|"  + service.getTag();
        String checkId = String.format("%s:%s:%s", key, service.getAddress(), service.getPort());
        if(logger.isTraceEnabled()) logger.trace("checkPass id = {}", checkId);
        Map<String, Object> map = new HashMap<>();
        map.put("id", checkId);
        map.put("pass", true);
        map.put("checkInterval", PortalRegistryConfig.load().getCheckInterval());
        String path = "/services/check";
        SimpleConnectionState.ConnectionToken connectionToken = null;
        try {
            connectionToken = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, JsonMapper.toJson(map));
            int statusCode = reference.get().getResponseCode();
            if (statusCode >= UNUSUAL_STATUS_CODE) {
                logger.error("checkPass error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            }
        } catch (Exception e) {
            logger.error("CheckPass request exception", e);
        } finally {
            if (connectionToken != null) client.restore(connectionToken);
        }
    }

    @Override
    public void checkFail(PortalRegistryService service, String token) {
        String key = service.getTag() == null ? service.getServiceId() : service.getServiceId() + "|"  + service.getTag();
        String checkId = String.format("%s:%s:%s", key, service.getAddress(), service.getPort());
        if(logger.isTraceEnabled()) logger.trace("checkFail id = {}", checkId);
        Map<String, Object> map = new HashMap<>();
        map.put("id", checkId);
        map.put("pass", false);
        map.put("checkInterval", PortalRegistryConfig.load().getCheckInterval());
        String path = "/services/check";
        SimpleConnectionState.ConnectionToken connectionToken = null;
        try {
            connectionToken = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, JsonMapper.toJson(map));
            int statusCode = reference.get().getResponseCode();
            if (statusCode >= UNUSUAL_STATUS_CODE) {
                logger.error("checkFail error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            }
        } catch (Exception e) {
            logger.error("CheckFail request exception", e);
        } finally {
            if (connectionToken != null) client.restore(connectionToken);
        }
    }

    @Override
    public void registerService(PortalRegistryService service, String token) {
        currentToken = token;
        registeredServices.put(service.getInstanceId(), service);
        try {
            ensureWebSocketConnected(token, notificationHandler);
            webSocketClient.sendRequest("controller.register", service.toRegisterParams());
        } catch (Exception e) {
            logger.error("Failed to register on portal controller websocket uri = {}", wsUri, e);
            scheduleReconnect();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void unregisterService(PortalRegistryService service, String token) {
        registeredServices.remove(service.getInstanceId());
        try {
            ensureWebSocketConnected(token, notificationHandler);
            Map<String, Object> params = new HashMap<>();
            params.put("serviceId", service.getServiceId());
            params.put("protocol", service.getProtocol());
            params.put("address", service.getAddress());
            params.put("port", service.getPort());
            if(service.getTag() != null) {
                params.put("tag", service.getTag());
            }
            webSocketClient.sendRequest("controller.deregister", params);
        } catch (Exception e) {
            logger.error("Failed to unregister on portal controller, Exception:", e);
        }
    }

    /**
     * to lookup health services based on serviceId and optional tag,
     *
     * @param serviceId       serviceId
     * @param tag             tag that is used for filtering
     * @param token           jwt token for security
     * @return null if serviceId is blank
     */
    @Override
    public List<Map<String, Object>> lookupHealthService(String serviceId, String tag, String token) {
        List<Map<String, Object>> services = null;
        if (StringUtils.isBlank(serviceId)) {
            return null;
        }
        SimpleConnectionState.ConnectionToken connectionToken = null;
        String path = "/services/lookup" + "?serviceId=" + serviceId;
        if (tag != null) {
            path = path + "&tag=" + tag;
        }
        if(logger.isTraceEnabled()) logger.trace("path = {}", path);
        try {
            connectionToken = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            AtomicReference<ClientResponse> reference = send(connection, Methods.GET, path, token, null);
            int statusCode = reference.get().getResponseCode();
            if (statusCode >= UNUSUAL_STATUS_CODE) {
                logger.error("Failed to look up service on Portal with serviceId {} tag {} response code {} and body {}", serviceId, tag, statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                throw new Exception("Failed to lookup service on Portal: " + statusCode);
            } else {
                String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                services = JsonMapper.string2List(body);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            if (connectionToken != null) client.restore(connectionToken);
        }
        return services;
    }

    @Override
    public boolean supportsWebSocket() {
        return true;
    }

    @Override
    public synchronized void ensureWebSocketConnected(String token, Consumer<Map<String, Object>> notificationHandler) {
        currentToken = token;
        this.notificationHandler = notificationHandler;
        if (webSocketClient == null || !webSocketClient.isOpen()) {
            webSocketClient = new PortalRegistryWebSocketClient(wsUri, token);
            webSocketClient.setDisconnectHandler(this::scheduleReconnect);
            webSocketClient.setConnectHandler(this::handleReconnectSuccess);
        }
        webSocketClient.setNotificationHandler(notificationHandler);
        try {
            webSocketClient.connect();
        } catch (Exception e) {
            scheduleReconnect();
            throw new RuntimeException("Failed to connect websocket to controller", e);
        }
    }

    @Override
    public void closeWebSocket() {
        PortalRegistryWebSocketClient current = webSocketClient;
        if (current != null) {
            current.close();
        }
        reconnectScheduled.set(false);
    }

    @Override
    public List<Map<String, Object>> subscribeService(String serviceId, String tag, String token) {
        currentToken = token;
        subscriptions.put(subscriptionKey(serviceId, tag), new SubscriptionState(serviceId, tag));
        ensureWebSocketConnected(token, notificationHandler);
        Map<String, Object> params = new HashMap<>();
        params.put("serviceId", serviceId);
        if (tag != null) {
            params.put("tag", tag);
        }
        try {
            Map<String, Object> result = webSocketClient.sendRequest("controller.subscribe", params);
            Object nodes = result.get("nodes");
            if (nodes instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        } catch (RuntimeException e) {
            scheduleReconnect();
            throw e;
        }
        return Collections.emptyList();
    }

    @Override
    public void unsubscribeService(String serviceId, String tag, String token) {
        subscriptions.remove(subscriptionKey(serviceId, tag));
        PortalRegistryWebSocketClient current = webSocketClient;
        if (current == null || !current.isOpen()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("serviceId", serviceId);
        if (tag != null) {
            params.put("tag", tag);
        }
        try {
            current.sendRequest("controller.unsubscribe", params);
        } catch (RuntimeException e) {
            scheduleReconnect();
            throw e;
        }
    }

    /**
     * send to portal controller with the passed in connection
     *
     * @param connection ClientConnection
     * @param method     http method to use
     * @param path       path to send to controller
     * @param token      token to put in header
     * @param json       request body to send
     * @return AtomicReference<ClientResponse> response
     */
    AtomicReference<ClientResponse> send(ClientConnection connection, HttpString method, String path, String token, String json) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();

        ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
        request.getRequestHeaders().put(Headers.HOST, "localhost");
        if (token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, token); // token is a JWT with Bearer prefix
        logger.trace("The request sent to controller: {} = request header: {}, request body is empty", uri.toString(), request.toString());
        if (StringUtils.isBlank(json)) {
            connection.sendRequest(request, client.createClientCallback(reference, latch));
        } else {
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
        }
        latch.await();
        logger.trace("The response got from controller: {} = {}", uri.toString(), reference.get().toString());
        return reference;
    }

    private static String toWebSocketUrl(String portalUrl) {
        String lower = portalUrl.toLowerCase();
        if (lower.startsWith("https://")) {
            return "wss://" + portalUrl.substring("https://".length());
        } else if (lower.startsWith("http://")) {
            return "ws://" + portalUrl.substring("http://".length());
        }
        throw new IllegalArgumentException("Unsupported portal url: " + portalUrl);
    }

    private void scheduleReconnect() {
        if (currentToken == null || (registeredServices.isEmpty() && subscriptions.isEmpty())) {
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        reconnectExecutor.execute(() -> {
            long delay = 1000L;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                    synchronized (this) {
                        webSocketClient = new PortalRegistryWebSocketClient(wsUri, currentToken);
                        webSocketClient.setNotificationHandler(notificationHandler);
                        webSocketClient.setDisconnectHandler(this::scheduleReconnect);
                        webSocketClient.setConnectHandler(this::handleReconnectSuccess);
                        webSocketClient.connect();
                    }
                    reconnectScheduled.set(false);
                    return;
                } catch (Exception e) {
                    logger.warn("Failed to reconnect portal-registry websocket, retrying in {} ms", delay, e);
                    delay = Math.min(delay * 2, 30000L);
                }
            }
        });
    }

    private void handleReconnectSuccess() {
        if (!reconnectScheduled.get()) {
            return;
        }
        if (replayInProgress) {
            return;
        }
        replayInProgress = true;
        try {
            replayState();
        } finally {
            replayInProgress = false;
        }
    }

    private void replayState() {
        PortalRegistryWebSocketClient current = webSocketClient;
        if (current == null || !current.isOpen()) {
            return;
        }

        for (PortalRegistryService service : registeredServices.values()) {
            current.sendRequest("controller.register", service.toRegisterParams());
        }

        for (SubscriptionState subscription : subscriptions.values()) {
            Map<String, Object> params = new HashMap<>();
            params.put("serviceId", subscription.serviceId());
            if (subscription.tag() != null) {
                params.put("tag", subscription.tag());
            }
            current.sendRequest("controller.subscribe", params);
        }
    }

    private static String subscriptionKey(String serviceId, String tag) {
        return tag == null ? serviceId : serviceId + "|" + tag;
    }

    private record SubscriptionState(String serviceId, String tag) {
    }
}
