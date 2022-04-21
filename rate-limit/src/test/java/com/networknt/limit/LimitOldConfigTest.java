package com.networknt.limit;

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
import io.undertow.util.Headers;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Test with the old limit.yml config file to ensure the backward compatibility.
 *
 * @author Steve Hu
 */

public class LimitOldConfigTest {
    static final Logger logger = LoggerFactory.getLogger(LimitHandlerTest.class);
    static final LimitConfig config = LimitConfig.load("limit-old");
    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception{
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            LimitHandler limitHandler = new LimitHandler(config);
            limitHandler.setNext(handler);
            handler = limitHandler;
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
                .add(Methods.GET, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testOneRequest() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.GET);
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
            Assert.assertEquals("OK", body);
        }
    }

    public String callApi() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
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
    // For some reason, travis become really slow or not allow multi-thread anymore and this test fails always.
    // You can run it within the IDE or remove the @Ignore and run it locally with mvn clean install.
    public void testMoreRequests() throws Exception {
        Callable<String> task = this::callApi;
        List<Callable<String>> tasks = Collections.nCopies(12, task);
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
        // make sure that there are at least one element in resultList is :503 or :429
        List<String> errorList = resultList.stream().filter(r->r.contains(":" + config.getErrorCode())).collect(Collectors.toList());
        Assert.assertTrue(errorList.size()>0);
    }

}
