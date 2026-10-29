package com.networknt.client;

import com.networknt.client.http.Http2ClientConnectionPool;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.Protocols;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xnio.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP/1 and prior-knowledge HTTP/2 exchanges, including response-body callbacks and cached reuse. */
class Http2ClientIoThreadLoopbackTest {
    private Undertow server;
    private XnioWorker worker;
    private Http2Client client;
    private final List<ClientConnection> connections = new CopyOnWriteArrayList<>();
    private final Http2ClientConnectionPool pool = Http2ClientConnectionPool.getInstance();

    @AfterEach
    void tearDown() throws Exception {
        for (ClientConnection connection : connections) connection.close();
        pool.clear();
        if (client != null) client.shutdown();
        if (worker != null) {
            worker.shutdown();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
        if (server != null) server.stop();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void warmColdAndConcurrentCallsKeepBodiesAndReuse(boolean http2, boolean body) throws Exception {
        Undertow.Builder builder = Undertow.builder().setIoThreads(1).setWorkerThreads(4)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setHandler(exchange -> {
                    exchange.getRequestReceiver().receiveFullString((ex, text) -> {
                        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        ex.getResponseSender().send("reply:" + text);
                    });
                });
        builder.addHttpListener(0, "localhost");
        server = builder.build();
        server.start();
        int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
        URI target = URI.create((http2 ? "h2c-prior" : "http") + "://localhost:" + port);
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 2));
        AtomicInteger connects = new AtomicInteger();
        client = new Http2Client() {
            @Override
            public CompletableFuture<ClientConnection> connectAsync(URI uri, boolean enabled) {
                connects.incrementAndGet();
                return super.connectAsync(null, uri, worker, null, BUFFER_POOL,
                        enabled ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY)
                        .thenApply(connection -> { connections.add(connection); return connection; });
            }
        };
        pool.clear();
        // The first call is cold. The next requests must reuse a completed cached connection.
        for (int i = 0; i < 8; i++) assertResponse(client, target, http2, body, i).get(10, TimeUnit.SECONDS);
        assertEquals(1, connects.get(), "serial requests should reuse the cached connection");
        List<CompletableFuture<Void>> pending = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            int requestId = i;
            pending.add(CompletableFuture.supplyAsync(() -> assertResponse(client, target, http2, body, requestId))
                    .thenCompose(future -> future));
        }
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(15, TimeUnit.SECONDS);
        if (http2) assertEquals(1, connects.get(), "HTTP/2 calls should multiplex the warm connection");
    }

    private CompletableFuture<Void> assertResponse(Http2Client client, URI target, boolean http2, boolean body, int id) {
        String content = "request-" + id;
        ClientRequest request = new ClientRequest().setMethod(body ? Methods.POST : Methods.GET).setPath("/echo");
        if (body) request.getRequestHeaders().put(Headers.CONTENT_LENGTH, Integer.toString(content.length()));
        return client.callService(target, request, body ? Optional.of(content) : Optional.empty(), http2)
                .thenAccept(response -> {
                    assertEquals(200, response.getResponseCode());
                    assertEquals(http2 ? Protocols.HTTP_2_0 : Protocols.HTTP_1_1, response.getProtocol());
                    assertEquals("reply:" + (body ? content : ""), response.getAttachment(Http2Client.RESPONSE_BODY));
                });
    }
}
