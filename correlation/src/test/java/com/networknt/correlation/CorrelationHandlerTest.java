/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.correlation;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.HttpStringConstants;
import io.undertow.Handlers;
import io.undertow.Undertow;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by steve on 23/09/16.
 */
public class CorrelationHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(CorrelationHandlerTest.class);

    static Undertow server = null;
    private static final String DEFAULT_CORRELATION_MDC_FIELD = "cId";
    private static final String DEFAULT_TRACEABILITY_MDC_FIELD = "tId";

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            CorrelationHandler correlationHandler = new CorrelationHandler();
            correlationHandler.setNext(handler);
            handler = correlationHandler;
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
                .add(Methods.GET, "/get", exchange -> exchange.getResponseSender().send("get"))
                .add(Methods.POST, "/post", exchange -> exchange.getResponseSender().send("post"))
                .add(Methods.GET, "/with", exchange -> {
                    String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
                    exchange.getResponseSender().send(cid);
                })
                .add(Methods.GET, "/without", exchange -> {
                    String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
                    exchange.getResponseSender().send(cid);
                })
                .add(Methods.GET, "/withoutNoAutogen", exchange -> {
                    String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
                    exchange.getResponseSender().send(cid == null ? "noCID" : "generated cid, error");
                });
    }

    @Test
    public void testWithCid() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/with").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, "cid");
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
        Assert.assertEquals("cid", body);
    }

    @Test
    public void testGetWithoutCid() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/without").setMethod(Methods.GET);
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
        Assert.assertNotNull(body);
        System.out.println("correlationId = " + body);
    }

    @Test
    public void testGetWithoutCidNoAutogen() throws Exception {
    	// reset the autogen of the correlation ID
    	CorrelationHandler.config.setAutogenCorrelationID(false);
        CorrelationHandler.config.setCorrelationMdcField(DEFAULT_CORRELATION_MDC_FIELD);
        CorrelationHandler.config.setTraceabilityMdcField(DEFAULT_TRACEABILITY_MDC_FIELD);

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
            ClientRequest request = new ClientRequest().setPath("/withoutNoAutogen").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }

        // set the autogen of the correlation ID to default value
        CorrelationHandler.config.setAutogenCorrelationID(true);

        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertNotNull(body);
        Assert.assertEquals("noCID", body);
        System.out.println("correlationId = " + body);
    }

    @Test
    public void testGetWithTid() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        CorrelationHandler.config.setCorrelationMdcField(DEFAULT_CORRELATION_MDC_FIELD);
        CorrelationHandler.config.setTraceabilityMdcField(DEFAULT_TRACEABILITY_MDC_FIELD);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/get").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, "12345");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            Assert.assertNotNull(body);
            Assert.assertEquals("get", body);
            String tid = reference.get().getResponseHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
            Assert.assertEquals("12345", tid);
        }
    }

    @Test
    public void testGetWithoutTid() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        CorrelationHandler.config.setCorrelationMdcField(DEFAULT_CORRELATION_MDC_FIELD);
        CorrelationHandler.config.setTraceabilityMdcField(DEFAULT_TRACEABILITY_MDC_FIELD);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/get").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            //request.getRequestHeaders().put(Constants.TRACEABILITY_ID, "12345");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            Assert.assertNotNull(body);
            Assert.assertEquals("get", body);
            String tid = reference.get().getResponseHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
            Assert.assertNull(tid);
        }
    }

    @Test
    public void testPostWithTid() throws Exception {
        CorrelationHandler.config.setCorrelationMdcField(DEFAULT_CORRELATION_MDC_FIELD);
        CorrelationHandler.config.setTraceabilityMdcField(DEFAULT_TRACEABILITY_MDC_FIELD);
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
            String post = "post";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/post");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, "12345");
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
            Assert.assertNotNull(body);
            Assert.assertEquals("post", body);
            String tid = reference.get().getResponseHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
            Assert.assertEquals("12345", tid);
        }
    }
}
