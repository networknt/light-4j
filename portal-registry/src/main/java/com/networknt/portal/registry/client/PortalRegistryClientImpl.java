package com.networknt.portal.registry.client;

import com.networknt.portal.registry.PortalRegistryConfig;
import com.networknt.portal.registry.PortalRegistryService;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PortalRegistryClientImpl implements PortalRegistryClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistryClientImpl.class);
    private final PortalRegistryConfig config;
    private final URI discoveryWsUri;
    private final URI microserviceWsUri;
    private final Map<String, PortalRegistryService> registeredServices = new ConcurrentHashMap<>();
    private final Map<String, PortalRegistryWebSocketClient> registrationClients = new ConcurrentHashMap<>();
    private final Map<String, SubscriptionState> subscriptions = new ConcurrentHashMap<>();
    private final Set<String> registrationReconnects = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "portal-registry-ws-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private volatile PortalRegistryWebSocketClient webSocketClient;
    private volatile Consumer<Map<String, Object>> notificationHandler;
    private volatile String currentToken;
    private volatile boolean replayInProgress;

    public PortalRegistryClientImpl() {
        this.config = PortalRegistryConfig.load();
        String portalUrl = config.getPortalUrl().toLowerCase();
        logger.debug("url = {}", portalUrl);
        try {
            this.discoveryWsUri = new URI(toWebSocketUrl(portalUrl) + "/ws/discovery");
            this.microserviceWsUri = new URI(toWebSocketUrl(portalUrl) + "/ws/microservice");
        } catch (URISyntaxException e) {
            logger.error("Invalid URI {}", portalUrl, e);
            throw new RuntimeException("Invalid URI " + portalUrl, e);
        }
    }

    @Override
    public void checkPass(PortalRegistryService service, String token) {
        logger.debug("checkPass ignored for controller-rs backend serviceId={}", service.getServiceId());
    }

    @Override
    public void checkFail(PortalRegistryService service, String token) {
        logger.debug("checkFail ignored for controller-rs backend serviceId={}", service.getServiceId());
    }

    @Override
    public void registerService(PortalRegistryService service, String token) {
        currentToken = token;
        registeredServices.put(service.getInstanceId(), service);
        try {
            registerControllerRsService(service, token);
        } catch (Exception e) {
            logger.error("Failed to register service {}", service.getServiceId(), e);
            scheduleServiceReconnect(service.getInstanceId());
            throw new RuntimeException("Failed to register service " + service.getServiceId(), e);
        }
    }

    @Override
    public void unregisterService(PortalRegistryService service, String token) {
        registeredServices.remove(service.getInstanceId());

        PortalRegistryWebSocketClient registrationClient = registrationClients.remove(service.getInstanceId());
        if (registrationClient != null) {
            registrationClient.close();
        }
        registrationReconnects.remove(service.getInstanceId());
    }

    @Override
    public List<Map<String, Object>> lookupHealthService(String serviceId, String tag, String token) {
        if (StringUtils.isBlank(serviceId)) {
            return null;
        }
        ensureWebSocketConnected(token, notificationHandler);
        Map<String, Object> result = webSocketClient.sendRequest("discovery/lookup", discoveryParams(serviceId, tag, null));
        return extractNodes(result);
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
            webSocketClient = new PortalRegistryWebSocketClient(discoveryWsUri, targetDiscoveryAuthorization(token));
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
        registrationClients.values().forEach(PortalRegistryWebSocketClient::close);
        registrationClients.clear();
        registeredServices.clear();
        subscriptions.clear();
        registrationReconnects.clear();
        notificationHandler = null;
        currentToken = null;
        reconnectScheduled.set(false);
    }

    @Override
    public List<Map<String, Object>> subscribeService(String serviceId, String tag, String token) {
        return subscribeService(serviceId, tag, null, token);
    }

    @Override
    public List<Map<String, Object>> subscribeService(String serviceId, String tag, String protocol, String token) {
        currentToken = token;
        subscriptions.put(subscriptionKey(serviceId, tag, protocol), new SubscriptionState(serviceId, tag, protocol));
        ensureWebSocketConnected(token, notificationHandler);
        try {
            return extractNodes(webSocketClient.sendRequest("discovery/subscribe", discoveryParams(serviceId, tag, protocol)));
        } catch (RuntimeException e) {
            scheduleReconnect();
            throw e;
        }
    }

    @Override
    public void unsubscribeService(String serviceId, String tag, String token) {
        unsubscribeService(serviceId, tag, null, token);
    }

    @Override
    public void unsubscribeService(String serviceId, String tag, String protocol, String token) {
        subscriptions.remove(subscriptionKey(serviceId, tag, protocol));
        PortalRegistryWebSocketClient current = webSocketClient;
        if (current == null || !current.isOpen()) {
            return;
        }
        try {
            current.sendRequest("discovery/unsubscribe", discoveryParams(serviceId, tag, protocol));
        } catch (RuntimeException e) {
            scheduleReconnect();
            throw e;
        }
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
                        webSocketClient = new PortalRegistryWebSocketClient(discoveryWsUri, targetDiscoveryAuthorization(currentToken));
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
        if (!reconnectScheduled.get() || replayInProgress) {
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

        registrationClients.values().forEach(PortalRegistryWebSocketClient::close);
        registrationClients.clear();
        for (PortalRegistryService service : registeredServices.values()) {
            try {
                registerControllerRsService(service, currentToken);
            } catch (RuntimeException e) {
                logger.warn("Failed to replay microservice registration for {}", service.getServiceId(), e);
                scheduleServiceReconnect(service.getInstanceId());
            }
        }

        for (SubscriptionState subscription : subscriptions.values()) {
            current.sendRequest("discovery/subscribe", discoveryParams(subscription.serviceId(), subscription.tag(), subscription.protocol()));
        }
    }

    private void registerControllerRsService(PortalRegistryService service, String token) {
        PortalRegistryWebSocketClient existing = registrationClients.remove(service.getInstanceId());
        if (existing != null) {
            existing.close();
        }

        PortalRegistryWebSocketClient registrationClient = new PortalRegistryWebSocketClient(microserviceWsUri, null);
        registrationClient.setDisconnectHandler(() -> scheduleServiceReconnect(service.getInstanceId()));
        try {
            registrationClient.connect();
            registrationClient.sendRequest("service/register", service.toControllerRsRegisterParams(stripBearerPrefix(token)));
        } catch (IOException e) {
            registrationClient.close();
            throw new RuntimeException("Failed to connect controller-rs microservice websocket", e);
        }
        registrationClients.put(service.getInstanceId(), registrationClient);
        registrationReconnects.remove(service.getInstanceId());
    }

    private void scheduleServiceReconnect(String instanceId) {
        if (currentToken == null || !registrationReconnects.add(instanceId)) {
            return;
        }
        reconnectExecutor.execute(() -> {
            long delay = 1000L;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    PortalRegistryService service = registeredServices.get(instanceId);
                    if (service == null || currentToken == null) {
                        return;
                    }
                    PortalRegistryWebSocketClient current = registrationClients.get(instanceId);
                    if (current != null && current.isOpen()) {
                        return;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                        registerControllerRsService(service, currentToken);
                        return;
                    } catch (Exception e) {
                        logger.warn("Failed to reconnect controller-rs microservice websocket for {}, retrying in {} ms", service.getServiceId(), delay, e);
                        delay = Math.min(delay * 2, 30000L);
                    }
                }
            } finally {
                registrationReconnects.remove(instanceId);
            }
        });
    }

    private String targetDiscoveryAuthorization(String token) {
        String discoveryToken = config.getControllerDiscoveryToken();
        return normalizeBearerToken(StringUtils.isBlank(discoveryToken) ? token : discoveryToken);
    }

    private Map<String, Object> discoveryParams(String serviceId, String tag, String protocol) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("serviceId", serviceId);
        if (tag != null) {
            params.put("envTag", tag);
        }
        if (protocol != null) {
            params.put("protocol", protocol);
        }
        return params;
    }

    private List<Map<String, Object>> extractNodes(Map<String, Object> result) {
        Object nodes = result.get("nodes");
        if (!(nodes instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> converted = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                converted.add(castMap(map));
            }
        }
        return converted;
    }

    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

    private String normalizeBearerToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "Bearer " + token.substring(7);
        }
        return "Bearer " + token;
    }

    private String stripBearerPrefix(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7);
        }
        return token;
    }

    private static String subscriptionKey(String serviceId, String tag, String protocol) {
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        return protocol == null ? key : key + "|" + protocol;
    }

    private record SubscriptionState(String serviceId, String tag, String protocol) {
    }
}
