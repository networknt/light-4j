package com.networknt.reqtrans;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.RequestInterceptorInjectionHandler;
import com.networknt.httpstring.AttachmentConstants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.connector.PooledByteBuffer;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RequestTransformerHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(RequestTransformerHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            RequestInterceptorInjectionHandler requestInjection = new RequestInterceptorInjectionHandler();
            requestInjection.setNext(handler);
            handler = requestInjection;
            server = Undertow.builder()
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
                .add(Methods.POST, "/post", exchange -> {
                    PooledByteBuffer[] bufferedData = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
                    String s = BuffersUtils.toString(bufferedData, StandardCharsets.UTF_8);
                    System.out.println("updated request body = " + s);
                    exchange.getResponseSender().send(s);
                });
    }
    @Test
    @Ignore
    public void testPostRequest() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String post = "original post";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/post");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    connection.sendRequest(request, client.createClientCallback(reference, latch, post));
                }
            });

            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assert.assertNotNull(body);
            Assert.assertEquals("[{\"com.networknt.handler.RequestInterceptorHandler\":[\"com.networknt.reqtrans.RequestTransformerHandler\"]}]", body);
        }
    }
}
