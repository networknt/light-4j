package com.networknt.token.limit;

import com.networknt.client.Http2Client;
import com.networknt.token.limit.TokenLimitHandler;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
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
import com.networknt.body.RequestBodyInterceptor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TokenLimitHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(TokenLimitHandlerTest.class);
    static Undertow server = null;
    //private static TokenLimitConfig config;
    String legacyRequestBody = "grant_type=client_credentials&client_id=legacyClient&client_secret=secret&scope=scope";
    String nonLegacyRequestBody = "grant_type=client_credentials&client_id=NonlegacyClient&client_secret=secret&scope=scope";

    @BeforeClass
    public static void setUp() throws Exception{
        if(server == null) {
            logger.info("starting server");
            //config = TokenLimitConfig.load("token-limit-template");
            RequestBodyInterceptor bodyHandler = new RequestBodyInterceptor();
            HttpHandler handler = getTestHandler();
            TokenLimitHandler tokenHandler = new TokenLimitHandler();
            tokenHandler.setNext(handler);
            bodyHandler.setNext(tokenHandler);
            server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(7080, "localhost")
                    .setHandler(bodyHandler)
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
        logger.info("getTestHandler");
        return Handlers.routing()
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("POST OK");
                });
    }

    public String callLegacyClient() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/oauth2/1234123/v1/token").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, legacyRequestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY) + ":" + reference.get().getResponseCode();
    }

    public String callNonLegacyClient() throws Exception {
        logger.info("callNonLegacyClient");
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            logger.info("creating connection");
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
            logger.info("connection created");
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        
        try {
            ClientRequest request = new ClientRequest().setPath("/oauth2/1234123/v1/token").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            logger.info("send request");
            connection.sendRequest(request, client.createClientCallback(reference, latch, nonLegacyRequestBody));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY) + ":" + reference.get().getResponseCode();
    }

    @Test
    public void testHandleRequest_NonLegacyClient() throws Exception {
        logger.info("starting NonLegacy calls");
        Callable<String> task = this::callNonLegacyClient;
        List<Callable<String>> tasks = Collections.nCopies(3, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            logger.info("calling");
            String s = future.get();
            logger.info("future = " + s);
            resultList.add(s);
        }
        long last = (System.currentTimeMillis() - start);
        // make sure that there are at least one element is 400
        List<String> errorList = resultList.stream().filter(r->r.contains(":400")).collect(Collectors.toList());
        logger.info("errorList size = " + errorList.size());
        Assert.assertTrue(errorList.size()>0);
    }
/*
    @Test
    public void testHandleRequest_LegacyClient() throws Exception {
        Callable<String> task = this::callLegacyClient;
        List<Callable<String>> tasks = Collections.nCopies(3, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            String s = future.get();
            logger.info("future = " + s);
            resultList.add(s);
        }
        long last = (System.currentTimeMillis() - start);
        // make sure that all requests returns 200 (no token limit applied)
        List<String> successList = resultList.stream().filter(r->r.contains(":200")).collect(Collectors.toList());
        logger.info("successList size = " + successList.size());
        Assert.assertTrue(successList.size()==3);
    }

    @Test
    public void testConvertStringToHashMap() {
        // Arrange
        String input = "grant_type=client_credentials&client_id=clientId&client_secret=secret&scope=scope";

        // Act
        Map<String, String> result = tokenLimitHandler.convertStringToHashMap(input);

        // Assert
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("client_credentials", result.get("grant_type"));
        Assert.assertEquals("clientId", result.get("client_id"));
        Assert.assertEquals("secret", result.get("client_secret"));
        Assert.assertEquals("scope", result.get("scope"));
    }

    @Test
    public void testMatchPath() {
        // Arrange
        String path = "/oauth2/1234123/v1/token";

        // Act
        boolean result = tokenLimitHandler.matchPath(path);

        // Assert
        Assert.assertTrue(result);
    }
         */
}
