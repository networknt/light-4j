package com.networknt.cors;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by stevehu on 2017-02-17.
 */
public class CorsHttpHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(CorsHttpHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            CorsHttpHandler corsHttpHandler = new CorsHttpHandler();
            corsHttpHandler.setNext(handler);
            handler = corsHttpHandler;
            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
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
                .add(Methods.GET, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                })
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testOptionsWrongOrigin() throws Exception {
        String url = "http://localhost:8080";
        Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.OPTIONS);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("Origin"), "http://example.com");
            request.getRequestHeaders().put(new HttpString("Access-Control-Request-Method"), "POST");
            request.getRequestHeaders().put(new HttpString("Access-Control-Request-Headers"), "X-Requested-With");
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
        HeaderMap headerMap = reference.get().getResponseHeaders();
        String header = headerMap.getFirst("Access-Control-Allow-Origin");
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            Assert.assertNull(header);
        }
    }

    @Test
    public void testOptionsCorrectOrigin() throws Exception {
        String url = "http://localhost:8080";
        Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.OPTIONS);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("Origin"), "http://localhost");
            request.getRequestHeaders().put(new HttpString("Access-Control-Request-Method"), "POST");
            request.getRequestHeaders().put(new HttpString("Access-Control-Request-Headers"), "X-Requested-With");
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
        HeaderMap headerMap = reference.get().getResponseHeaders();
        String header = headerMap.getFirst("Access-Control-Allow-Origin");
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            Assert.assertNotNull(header);
        }
    }
}
