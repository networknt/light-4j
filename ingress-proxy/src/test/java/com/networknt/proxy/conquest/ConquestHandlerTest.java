package com.networknt.proxy.conquest;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import com.networknt.proxy.salesforce.SalesforceConfig;
import com.networknt.proxy.salesforce.SalesforceHandler;
import com.networknt.proxy.salesforce.SalesforceHandlerTest;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ConquestHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(ConquestHandlerTest.class);
    static final ConquestConfig config = ConquestConfig.load();
    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception{
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            ConquestHandler conquestHandler = new ConquestHandler();
            conquestHandler.setNext(handler);
            handler = conquestHandler;
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
                });
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
            ClientRequest request = new ClientRequest().setPath("/conquest").setMethod(Methods.GET);
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
            Assert.assertEquals("GET OK", body);
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
        String requestBody = "{\"clientId\":\"FSC_0030303343303x32AA2\",\"loggedInUserEmail\":\"steve.hu@networknt.com\"}";
        try {
            ClientRequest request = new ClientRequest().setPath("/services/apexrest/NNT_ConquestApplicationServices/getAccountDetailsForConquestToUpdatePlan").setMethod(Methods.POST);
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

}
