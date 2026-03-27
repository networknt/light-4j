package com.networknt.portal.registry.client;

import com.networknt.client.Http2Client;
import com.networknt.config.JsonMapper;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PortalRegistryWebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistryWebSocketClient.class);
    private static final long REQUEST_TIMEOUT_MILLIS = 5000L;

    private final URI uri;
    private final String authorization;
    private final UndertowXnioSsl ssl;
    private final AtomicLong nextId = new AtomicLong(1L);
    private final Map<String, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();

    private volatile WebSocketChannel channel;
    private volatile Consumer<Map<String, Object>> notificationHandler;
    private volatile Runnable disconnectHandler;
    private volatile Runnable connectHandler;
    private volatile boolean explicitClose;

    public PortalRegistryWebSocketClient(URI uri, String authorization) {
        this.uri = uri;
        this.authorization = authorization;
        try {
            this.ssl = new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, Http2Client.createSSLContext());
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize websocket SSL context", e);
        }
    }

    public synchronized void connect() throws IOException {
        if (isOpen()) {
            return;
        }
        explicitClose = false;

        WebSocketClientNegotiation negotiation = new WebSocketClientNegotiation(Collections.emptyList(), Collections.emptyList()) {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                if (authorization != null) {
                    headers.put("Authorization", Collections.singletonList(authorization));
                }
            }
        };

        io.undertow.websockets.client.WebSocketClient.ConnectionBuilder connectionBuilder =
                io.undertow.websockets.client.WebSocketClient.connectionBuilder(Http2Client.WORKER, Http2Client.BUFFER_POOL, uri)
                        .setSsl(ssl)
                        .setClientNegotiation(negotiation);
        IoFuture<WebSocketChannel> future = connectionBuilder.connect();
        try {
            channel = future.get();
        } catch (IOException e) {
            throw e;
        }
        initReceiveLoop(channel);
        Runnable handler = connectHandler;
        if (handler != null) {
            handler.run();
        }
    }

    public void setNotificationHandler(Consumer<Map<String, Object>> notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    public void setDisconnectHandler(Runnable disconnectHandler) {
        this.disconnectHandler = disconnectHandler;
    }

    public void setConnectHandler(Runnable connectHandler) {
        this.connectHandler = connectHandler;
    }

    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    public synchronized void close() {
        if (channel == null) {
            return;
        }
        explicitClose = true;
        try {
            channel.sendClose();
        } catch (IOException e) {
            logger.debug("Error while closing websocket", e);
        } finally {
            channel = null;
        }
    }

    public Map<String, Object> sendRequest(String method, Map<String, Object> params) {
        try {
            connect();
            String id = String.valueOf(nextId.getAndIncrement());
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            pending.put(id, future);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", id);
            request.put("method", method);
            request.put("params", params);

            send(JsonMapper.toJson(request));
            Map<String, Object> response = future.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            Object error = response.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                throw new RuntimeException(String.valueOf(errorMap.get("message")));
            }
            Object result = response.get("result");
            if (result instanceof Map<?, ?> resultMap) {
                return castMap(resultMap);
            }
            return Collections.emptyMap();
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to invoke websocket method " + method, e);
        }
    }

    public void sendNotification(String method, Map<String, Object> params) {
        try {
            connect();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", method);
            request.put("params", params);
            send(JsonMapper.toJson(request));
        } catch (IOException e) {
            throw new RuntimeException("Failed to invoke websocket notification " + method, e);
        }
    }

    private void send(String text) throws IOException {
        WebSocketChannel current = channel;
        if (current == null || !current.isOpen()) {
            throw new IOException("Websocket channel is not open");
        }
        WebSockets.sendTextBlocking(text, current);
    }

    private void initReceiveLoop(WebSocketChannel currentChannel) {
        currentChannel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel ws, BufferedTextMessage message) {
                handleMessage(message.getData());
            }

            @Override
            protected void onError(WebSocketChannel ws, Throwable error) {
                logger.error("WebSocket client error", error);
                failPending(error);
            }
        });
        currentChannel.resumeReceives();
        currentChannel.addCloseTask(ws -> failPending(new IOException("Websocket closed: " + ws.getCloseReason())));
    }

    private void handleMessage(String message) {
        Map<String, Object> envelope = JsonMapper.string2Map(message);
        if (envelope == null) {
            return;
        }

        Object method = envelope.get("method");
        if (method != null) {
            Consumer<Map<String, Object>> handler = notificationHandler;
            if (handler != null) {
                handler.accept(envelope);
            }
            return;
        }

        Object id = envelope.get("id");
        if (id != null) {
            CompletableFuture<Map<String, Object>> future = pending.remove(String.valueOf(id));
            if (future != null) {
                future.complete(envelope);
            }
        }
    }

    private void failPending(Throwable error) {
        channel = null;
        for (Map.Entry<String, CompletableFuture<Map<String, Object>>> entry : pending.entrySet()) {
            entry.getValue().completeExceptionally(error);
        }
        pending.clear();
        if (!explicitClose) {
            Runnable handler = disconnectHandler;
            if (handler != null) {
                handler.run();
            }
        }
    }

    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }
}
