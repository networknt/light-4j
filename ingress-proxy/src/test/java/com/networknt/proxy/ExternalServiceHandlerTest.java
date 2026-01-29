package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import com.networknt.proxy.salesforce.SalesforceConfig;
import com.networknt.proxy.salesforce.SalesforceHandler;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalServiceHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(ExternalServiceHandlerTest.class);
    static final ExternalServiceConfig config = new ExternalServiceConfig();
    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception{
        if(server == null) {
            logger.info("starting serverconfig");
            HttpHandler handler = getTestHandler();
            ExternalServiceHandler externalServiceHandler = new ExternalServiceHandler();
            externalServiceHandler.setNext(handler);
            handler = externalServiceHandler;
            server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(7080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
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
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("POST OK");
                })
                .add(Methods.GET, "/", exchange -> {
                    exchange.getResponseSender().send("GET OK");
                })
                .add(Methods.GET, "/timeout-test", exchange -> {
                    exchange.getResponseSender().send("GET timeout-test OK");
                })
                .add(Methods.POST, "/timeout-test", exchange -> {
                    exchange.getResponseSender().send("POST timeout-test OK");
                })
                .add(Methods.GET, "/timeout-test/slow", exchange -> {
                    // Simulate a slow response that exceeds the configured timeout
                    try {
                        Thread.sleep(2000);
                        if (!Thread.currentThread().isInterrupted()) {
                            exchange.getResponseSender().send("This should timeout");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // Don't send response if interrupted
                    }
                });
    }

    /**
     * Helper method to create a test connection to the local test server.
     * Reduces code duplication across test methods.
     */
    private static ClientConnection createTestConnection() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        try {
            return client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Test
    @Ignore
    public void testOneGetRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/get").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            Assert.assertTrue(body.contains("https://postman-echo.com/get"));
        }
    }

    @Test
    @Ignore
    public void testOnePostRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "{\"key\": \"key1\"}";
        try {
            ClientRequest request = new ClientRequest().setPath("/post").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("statusCode = " + statusCode + " body = " + body);
        Assert.assertEquals(200, statusCode);
    }

    @Test
    @Ignore
    public void testEmptyPostBodyRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "";
        try {
            ClientRequest request = new ClientRequest().setPath("/post").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("statusCode = " + statusCode + " body = " + body);
        Assert.assertEquals(400, statusCode);
    }

    @Test
    public void testPathPrefixGetRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = createTestConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/timeout-test").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertTrue(body.contains("GET timeout-test OK"));
    }

    @Test
    public void testPathPrefixPostRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = createTestConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "{\"key\": \"value\"}";
        try {
            ClientRequest request = new ClientRequest().setPath("/timeout-test").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertTrue(body.contains("POST timeout-test OK"));
    }

    @Test
    public void testPathPrefixTimeout() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = createTestConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        boolean timeoutOccurred = false;
        try {
            ClientRequest request = new ClientRequest().setPath("/timeout-test/slow").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            // Wait for response with a timeout longer than the configured pathPrefix timeout (1000ms)
            // to ensure we detect if the timeout mechanism fails
            boolean completed = latch.await(3000, TimeUnit.MILLISECONDS);

            if (!completed) {
                // Latch timed out - timeout may have occurred at a different level
                timeoutOccurred = true;
            } else if (reference.get() != null) {
                // If we got a response, check if it's a timeout error status
                int statusCode = reference.get().getResponseCode();
                // A 504 Gateway Timeout or 408 Request Timeout indicates the timeout was enforced
                timeoutOccurred = (statusCode == 504 || statusCode == 408);
            }
        } catch (Exception e) {
            logger.info("Expected timeout exception occurred: ", e);
            // Check if this is a timeout-related exception
            boolean isTimeoutException = e instanceof HttpTimeoutException ||
                    (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"));
            timeoutOccurred = isTimeoutException;
        } finally {
            IoUtils.safeClose(connection);
        }
        Assert.assertTrue("Expected timeout to occur for slow endpoint with configured pathPrefix timeout", timeoutOccurred);
    }


}
