package com.networknt.portal.registry.client;

import com.networknt.client.Http2Client;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PortalRegistryWebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistryWebSocketClient.class);
    private static final long REQUEST_TIMEOUT_MILLIS = 5000L;

    private final URI uri;
    private final String authorization;
    private final HttpClient httpClient;
    private final AtomicLong nextId = new AtomicLong(1L);
    private final Map<String, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean open = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile Consumer<Map<String, Object>> notificationHandler;
    private volatile Runnable disconnectHandler;
    private volatile Runnable connectHandler;
    private volatile boolean explicitClose;

    public PortalRegistryWebSocketClient(URI uri, String authorization) {
        this.uri = uri;
        this.authorization = authorization;
        Http2Client.getInstance();
        try {
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(Http2Client.createSSLContext())
                    .connectTimeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MILLIS))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize websocket SSL context", e);
        }
    }

    public synchronized void connect() throws IOException {
        if (isOpen()) {
            return;
        }
        explicitClose = false;

        PortalWebSocketListener listener = new PortalWebSocketListener();
        java.net.http.WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MILLIS));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }

        try {
            webSocket = builder.buildAsync(uri, listener).join();
            open.set(true);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to connect websocket to " + uri, cause == null ? e : cause);
        }

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
        return open.get() && webSocket != null;
    }

    public synchronized void close() {
        WebSocket current = webSocket;
        if (current == null) {
            return;
        }
        explicitClose = true;
        try {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "closed").join();
        } catch (CompletionException e) {
            logger.debug("Error while closing websocket", e.getCause() == null ? e : e.getCause());
        } finally {
            webSocket = null;
            open.set(false);
        }
    }

    public Map<String, Object> sendRequest(String method, Map<String, Object> params) {
        String id = String.valueOf(nextId.getAndIncrement());
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            connect();
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to invoke websocket method " + method, e);
        } catch (IOException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to invoke websocket method " + method, e);
        } finally {
            pending.remove(id);
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
        WebSocket current = webSocket;
        if (current == null || !open.get()) {
            throw new IOException("Websocket channel is not open");
        }
        try {
            current.sendText(text, true).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to send websocket message", cause == null ? e : cause);
        }
    }

    private void handleMessage(String message) {
        Map<String, Object> envelope;
        try {
            envelope = JsonMapper.string2Map(message);
        } catch (RuntimeException e) {
            logger.warn("Unable to parse websocket message {}", message, e);
            return;
        }
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
        webSocket = null;
        open.set(false);
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

    private class PortalWebSocketListener implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String message = textBuffer.toString();
                textBuffer.setLength(0);
                handleMessage(message);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            failPending(new IOException("Websocket closed: " + statusCode + " " + reason));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.error("WebSocket client error", error);
            failPending(error);
        }
    }
}
