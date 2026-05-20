package com.networknt.handler;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class ProxyHandlerSseTest {

    private final List<Undertow> servers = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Undertow server : servers) {
            server.stop();
        }
        servers.clear();
    }

    @Test
    void shouldProxySseResponseAndOverwritePresetContentType() throws Exception {
        Undertow backend = startServer(sseHandler(0, 250, true));
        ProxyHandler proxy = buildProxy(backendUri(backend), builder -> builder
                .setMaxRequestTime(100)
                .setStreamRequestAcceptTypes(Collections.emptyList()));
        Undertow frontend = startServer(exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            proxy.handleRequest(exchange);
        });

        HttpURLConnection connection = open(frontendUri(frontend, "/events"), "application/json");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals(StatusCodes.OK, connection.getResponseCode());
            Assertions.assertTrue(connection.getHeaderField(Headers.CONTENT_TYPE_STRING).startsWith("text/event-stream"));
            Assertions.assertNull(connection.getHeaderField(Headers.CONTENT_LENGTH_STRING));
            Assertions.assertEquals("data: first", reader.readLine());
            Assertions.assertEquals("", reader.readLine());
            Assertions.assertEquals("data: second", reader.readLine());
        } finally {
            connection.disconnect();
        }
    }

    @Test
    void shouldUseStreamingTimeoutForConfiguredPathPrefixBeforeResponseHeaders() throws Exception {
        Undertow backend = startServer(sseHandler(250, 0, false));
        ProxyHandler proxy = buildProxy(backendUri(backend), builder -> builder
                .setMaxRequestTime(100)
                .setStreamPathPrefixes(Collections.singletonList("/events"))
                .setStreamMaxRequestTime(0)
                .setStreamRequestAcceptTypes(Collections.emptyList()));
        Undertow frontend = startServer(proxy);

        HttpURLConnection connection = open(frontendUri(frontend, "/events"), "application/json");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals(StatusCodes.OK, connection.getResponseCode());
            Assertions.assertEquals("data: first", reader.readLine());
        } finally {
            connection.disconnect();
        }
    }

    @Test
    void pathPrefixTimeoutShouldNotMutateGlobalTimeout() throws Exception {
        Undertow backend = startServer(delayedTextHandler(150));
        Map<String, Integer> prefixTimeouts = new LinkedHashMap<>();
        prefixTimeouts.put("/slow", 0);
        ProxyHandler proxy = buildProxy(backendUri(backend), builder -> builder
                .setMaxRequestTime(75)
                .setPathPrefixMaxRequestTime(prefixTimeouts)
                .setStreamRequestAcceptTypes(Collections.emptyList())
                .setStreamResponseContentTypes(Collections.emptyList()));
        Undertow frontend = startServer(proxy);

        HttpURLConnection slowConnection = open(frontendUri(frontend, "/slow"), "application/json");
        try {
            Assertions.assertEquals(StatusCodes.OK, slowConnection.getResponseCode());
        } finally {
            slowConnection.disconnect();
        }

        HttpURLConnection timeoutConnection = open(frontendUri(frontend, "/timeout"), "application/json");
        try {
            Assertions.assertEquals(StatusCodes.GATEWAY_TIME_OUT, timeoutConnection.getResponseCode());
        } finally {
            timeoutConnection.disconnect();
        }
    }

    @Test
    void shouldCloseStreamingResponseAfterIdleTimeout() throws Exception {
        Undertow backend = startServer(sseHandler(0, 1000, true));
        ProxyHandler proxy = buildProxy(backendUri(backend), builder -> builder
                .setMaxRequestTime(0)
                .setStreamPathPrefixes(Collections.singletonList("/events"))
                .setStreamIdleTimeout(100));
        Undertow frontend = startServer(proxy);

        HttpURLConnection connection = open(frontendUri(frontend, "/events"), "text/event-stream");
        long start = System.nanoTime();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals(StatusCodes.OK, connection.getResponseCode());
            Assertions.assertEquals("data: first", reader.readLine());
            Assertions.assertEquals("", reader.readLine());
            try {
                Assertions.assertNull(reader.readLine());
            } catch (IOException expected) {
                // Closing either side of the proxied stream is an acceptable idle-timeout result for the client.
            }
            Assertions.assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 900);
        } finally {
            connection.disconnect();
        }
    }

    private ProxyHandler buildProxy(URI backendUri, Consumer<ProxyHandler.Builder> customizer) {
        LoadBalancingProxyClient client = new LoadBalancingProxyClient()
                .addHost(backendUri)
                .setConnectionsPerThread(2);
        ProxyHandler.Builder builder = ProxyHandler.builder()
                .setProxyClient(client)
                .setRewriteHostHeader(false)
                .setMaxConnectionRetries(0);
        customizer.accept(builder);
        return builder.build();
    }

    private Undertow startServer(HttpHandler handler) {
        Undertow server = Undertow.builder()
                .addHttpListener(0, "localhost")
                .setHandler(handler)
                .build();
        server.start();
        servers.add(server);
        return server;
    }

    private URI backendUri(Undertow server) {
        InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
        return URI.create("http://localhost:" + address.getPort());
    }

    private URI frontendUri(Undertow server, String path) {
        return backendUri(server).resolve(path);
    }

    private HttpURLConnection open(URI uri, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(2000);
        connection.setRequestProperty(Headers.ACCEPT_STRING, accept);
        return connection;
    }

    private HttpHandler sseHandler(long delayBeforeHeadersMillis, long delayAfterFirstEventMillis, boolean sendSecondEvent) {
        return exchange -> exchange.dispatch(() -> {
            try {
                sleep(delayBeforeHeadersMillis);
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream; charset=utf-8");
                exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
                StreamSinkChannel channel = exchange.getResponseChannel();
                write(channel, "data: first\n\n");
                sleep(delayAfterFirstEventMillis);
                if (sendSecondEvent) write(channel, "data: second\n\n");
                channel.shutdownWrites();
                Channels.flushBlocking(channel);
            } catch (IOException ignored) {
                // Some tests intentionally close the proxy connection before the backend finishes.
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private HttpHandler delayedTextHandler(long delayMillis) {
        return exchange -> exchange.dispatch(() -> {
            try {
                sleep(delayMillis);
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                StreamSinkChannel channel = exchange.getResponseChannel();
                write(channel, "OK");
                channel.shutdownWrites();
                Channels.flushBlocking(channel);
            } catch (IOException ignored) {
                // The timeout test intentionally closes the proxied backend connection before this write.
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void write(StreamSinkChannel channel, String body) throws IOException {
        Channels.writeBlocking(channel, ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)));
        Channels.flushBlocking(channel);
    }

    private void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
