package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import com.networknt.client.RequestConfig;
import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.io.*;
import io.undertow.util.*;
import org.junit.jupiter.api.*;
import org.xnio.*;
import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP/1 CLOSE_REQ body delivery through Http2ClientConnectionPool; no GOAWAY-drain claim. */
class Http1ConnectionDrainLoopbackTest {
    private XnioWorker worker;
    private ByteBufferPool buffers;
    private Undertow server;
    private ClientConnection connection;
    private Http2ClientConnectionPool pool;
    private final CompletableFuture<HttpServerExchange> partial = new CompletableFuture<>();
    private final CompletableFuture<Void> headers = new CompletableFuture<>();
    private final CompletableFuture<String> body = new CompletableFuture<>();

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        buffers = new DefaultByteBufferPool(false, 16384);
        RequestConfig config = Config.getInstance().getMapper().convertValue(Map.of(
                ClientConfig.CONNECTION_POOL_SIZE, 4, ClientConfig.MAX_CONNECTION_NUM_PER_HOST, 4,
                ClientConfig.MIN_CONNECTION_NUM_PER_HOST, 1, ClientConfig.MAX_REQ_PER_CONN, -1,
                ClientConfig.CONNECTION_EXPIRE_TIME, 1_000_000), RequestConfig.class);
        pool = new Http2ClientConnectionPool(config, () -> 0);
        server = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1).setWorkerThreads(2)
                .setHandler(exchange -> exchange.dispatch(() -> {
                    exchange.getResponseHeaders().put(Headers.CONNECTION, "close");
                    exchange.setResponseContentLength("first-complete".length());
                    exchange.getResponseSender().send("first-", new IoCallback() {
                        @Override public void onComplete(HttpServerExchange response, Sender sender) { partial.complete(response); }
                        @Override public void onException(HttpServerExchange response, Sender sender, IOException error) {
                            partial.completeExceptionally(error);
                        }
                    });
                })).build();
        server.start();
    }
    @AfterEach void tearDown() throws Exception {
        if (pool != null) pool.clear();
        IoUtils.safeClose(connection);
        if (server != null) server.stop();
        if (worker != null) {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
        if (buffers != null) buffers.close();
    }
    @Test void connectionCloseHeaderDrainsTheResponseAfterPoolRetirement() throws Exception {
        int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
        URI uri = URI.create("http://localhost:" + port);
        CompletableFuture<ClientConnection> connected = new CompletableFuture<>();
        UndertowClient.getInstance().connect(new ClientCallback<>() {
            @Override public void completed(ClientConnection result) { connected.complete(result); }
            @Override public void failed(IOException error) { connected.completeExceptionally(error); }
        }, uri, worker, buffers, OptionMap.EMPTY);
        connection = connected.get(5, TimeUnit.SECONDS);
        pool.cacheConnection(uri, connection);
        CompletableFuture<Void> closed = new CompletableFuture<>();
        ClientConnection owned = IoThreadClientConnection.wrap(connection);
        owned.addCloseListener(ignored -> closed.complete(null));
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/");
        request.getRequestHeaders().put(Headers.HOST, "localhost");
        owned.sendRequest(request, responseCallback());
        HttpServerExchange backend = partial.get(5, TimeUnit.SECONDS);
        headers.get(5, TimeUnit.SECONDS);
        assertFalse(connection.isOpen(), "CLOSE_REQ stops admission while the response body is still arriving");
        assertFalse(body.isDone());
        assertNull(pool.getConnection(uri));
        CompletableFuture<Void> ownerBarrier = new CompletableFuture<>();
        connection.getIoThread().execute(() -> ownerBarrier.complete(null));
        ownerBarrier.get(5, TimeUnit.SECONDS);
        assertFalse(closed.isDone(), "pool retirement must not truncate the response");
        backend.getIoThread().execute(() -> backend.getResponseSender().send("complete"));
        assertEquals("first-complete", body.get(5, TimeUnit.SECONDS));
        pool.resetConnectionStatus(connection);
        closed.get(5, TimeUnit.SECONDS);
        assertEquals(0, pool.numberOfConnections());
    }
    private ClientCallback<ClientExchange> responseCallback() {
        return new ClientCallback<>() {
            @Override public void completed(ClientExchange exchange) {
                exchange.setResponseListener(new ClientCallback<>() {
                    @Override public void completed(ClientExchange response) {
                        new StringReadChannelListener(buffers) {
                            @Override protected void stringDone(String value) { body.complete(value); }
                            @Override protected void error(IOException error) { body.completeExceptionally(error); }
                        }.setup(response.getResponseChannel());
                        headers.complete(null);
                    }
                    @Override public void failed(IOException error) { body.completeExceptionally(error); }
                });
                new StringWriteChannelListener("") {
                    @Override protected void handleError(org.xnio.channels.StreamSinkChannel channel, IOException error) {
                        body.completeExceptionally(error);
                    }
                }.setup(exchange.getRequestChannel());
            }
            @Override public void failed(IOException error) { body.completeExceptionally(error); }
        };
    }
}
