package com.networknt.sse;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.util.Methods;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SseHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(SseHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if (server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            SseHandler sseHandler = new SseHandler();
            sseHandler.setNext(handler);
            handler = sseHandler;
            server = Undertow.builder()
                    .addHttpListener(7080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/health", exchange -> exchange.getResponseSender().send("OK"));
    }

    @Test
    public void testSseConnection() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;
        try {
            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/sse");
            request.getRequestHeaders().put(io.undertow.util.Headers.ACCEPT, "text/event-stream");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await(1000, TimeUnit.MILLISECONDS);

            // NOTE: Typical HTTP2Client usage waits for the entire response. SSE is an open stream.
            // This test mainly verifies we don't get an immediate error and that logic executes.
            // A more robust test requires a client that supports reading the stream chunk by chunk.

            // For now, let's just inspect the connections in Registry if possible.
            // Since the client call might block waiting for close, we might need a separate thread or check registry async.

            // Check registry
            Assert.assertFalse(SseConnectionRegistry.getConnections().isEmpty());

        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw e;
        } finally {
            client.restore(token);
        }
    }

    @Test
    public void testSseWithTraceabilityId() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/sse");
            request.getRequestHeaders().put(io.undertow.util.Headers.ACCEPT, "text/event-stream");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            String traceId = "test-id-123";
            request.getRequestHeaders().put(io.undertow.util.HttpString.tryFromString("X-Traceability-Id"), traceId);
            connection.sendRequest(request, client.createClientCallback(reference, latch));

            Thread.sleep(500); // Give time for connection to register

            ServerSentEventConnection conn = SseConnectionRegistry.getConnection(traceId);
            Assert.assertNotNull(conn);
            Assert.assertTrue(conn.isOpen());

        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw e;
        } finally {

            client.restore(token);

        }
    }

    @Test
    public void testSsePathPrefix() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/sse/test");
            request.getRequestHeaders().put(io.undertow.util.Headers.ACCEPT, "text/event-stream");
            request.getRequestHeaders().put(io.undertow.util.Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await(1000, TimeUnit.MILLISECONDS);

            // Check status code
            int statusCode = reference.get().getResponseCode();
            Assert.assertEquals(200, statusCode);

        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw e;
        } finally {

            client.restore(token);

        }
    }
}
