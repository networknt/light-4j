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

package com.networknt.header;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


/**
 *
 * @author Steve Hu
 */
public class HeaderHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(HeaderHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            HeaderHandler headerHandler = new HeaderHandler();
            if(headerHandler.isEnabled()) {
                headerHandler.setNext(handler);
                handler = headerHandler;
            }
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
                .add(Methods.GET, "/petstore", exchange -> {
                    Map<String, Map<String, String>> headers = new HashMap<>();
                    Map<String, String> requestHeaders = new HashMap<>();
                    String headerA = exchange.getRequestHeaders().getFirst("headerA");
                    if(headerA != null) requestHeaders.put("headerA", headerA);
                    String headerB = exchange.getRequestHeaders().getFirst("headerB");
                    if(headerB != null) requestHeaders.put("headerB", headerB);
                    String keyA = exchange.getRequestHeaders().getFirst("keyA");
                    if(keyA != null) requestHeaders.put("keyA", keyA);
                    String keyB = exchange.getRequestHeaders().getFirst("keyB");
                    if(keyB != null) requestHeaders.put("keyB", keyB);
                    headers.put("requestHeaders", requestHeaders);

                    Map<String, String> responseHeaders = new HashMap<>();
                    String headerC = exchange.getResponseHeaders().getFirst("headerC");
                    if(headerC != null) responseHeaders.put("headerC", headerC);
                    String headerD = exchange.getResponseHeaders().getFirst("headerD");
                    if(headerD != null) responseHeaders.put("headerD", headerD);
                    String keyC = exchange.getResponseHeaders().getFirst("keyC");
                    if(keyC != null) responseHeaders.put("keyC", keyC);
                    String keyD = exchange.getResponseHeaders().getFirst("keyD");
                    if(keyD != null) responseHeaders.put("keyD", keyD);
                    headers.put("responseHeaders", responseHeaders);
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(headers));
                })
                .add(Methods.GET, "/market", exchange -> {
                    Map<String, Map<String, String>> headers = new HashMap<>();
                    Map<String, String> requestHeaders = new HashMap<>();
                    String headerE = exchange.getRequestHeaders().getFirst("headerE");
                    if(headerE != null) requestHeaders.put("headerE", headerE);
                    String headerF = exchange.getRequestHeaders().getFirst("headerF");
                    if(headerF != null) requestHeaders.put("headerF", headerF);
                    String keyE = exchange.getRequestHeaders().getFirst("keyE");
                    if(keyE != null) requestHeaders.put("keyE", keyE);
                    String keyF = exchange.getRequestHeaders().getFirst("keyF");
                    if(keyF != null) requestHeaders.put("keyF", keyF);
                    headers.put("requestHeaders", requestHeaders);

                    Map<String, String> responseHeaders = new HashMap<>();
                    String headerG = exchange.getResponseHeaders().getFirst("headerG");
                    if(headerG != null) responseHeaders.put("headerG", headerG);
                    String headerH = exchange.getResponseHeaders().getFirst("headerH");
                    if(headerH != null) responseHeaders.put("headerH", headerH);
                    String keyG = exchange.getResponseHeaders().getFirst("keyG");
                    if(keyG != null) responseHeaders.put("keyG", keyG);
                    String keyH = exchange.getResponseHeaders().getFirst("keyH");
                    if(keyH != null) responseHeaders.put("keyH", keyH);
                    headers.put("responseHeaders", responseHeaders);
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(headers));
                })
                .add(Methods.GET, "/get", exchange -> {
                    Map<String, Map<String, String>> headers = new HashMap<>();
                    Map<String, String> requestHeaders = new HashMap<>();
                    String header1 = exchange.getRequestHeaders().getFirst("header1");
                    if(header1 != null) requestHeaders.put("header1", header1);
                    String header2 = exchange.getRequestHeaders().getFirst("header2");
                    if(header2 != null) requestHeaders.put("header2", header1);
                    String key1 = exchange.getRequestHeaders().getFirst("key1");
                    if(key1 != null) requestHeaders.put("key1", key1);
                    String key2 = exchange.getRequestHeaders().getFirst("key2");
                    if(key2 != null) requestHeaders.put("key2", key2);
                    headers.put("requestHeaders", requestHeaders);

                    Map<String, String> responseHeaders = new HashMap<>();
                    header1 = exchange.getResponseHeaders().getFirst("header1");
                    if(header1 != null) responseHeaders.put("header1", header1);
                    header2 = exchange.getResponseHeaders().getFirst("header2");
                    if(header2 != null) responseHeaders.put("header2", header1);
                    key1 = exchange.getResponseHeaders().getFirst("key1");
                    if(key1 != null) responseHeaders.put("key1", key1);
                    key2 = exchange.getResponseHeaders().getFirst("key2");
                    if(key2 != null) responseHeaders.put("key2", key2);
                    headers.put("responseHeaders", responseHeaders);
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(headers));
                });
    }

    @Test
    public void testRequestHeader() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/get").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("header1"), "header1");
            request.getRequestHeaders().put(new HttpString("header2"), "header2");
            request.getRequestHeaders().put(new HttpString("key1"), "old1");
            request.getRequestHeaders().put(new HttpString("key2"), "old2");
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
        Assert.assertEquals("{\"requestHeaders\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"responseHeaders\":{\"key1\":\"value1\",\"key2\":\"value2\"}}", body);
    }

    @Test
    public void testPetstoreHeader() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/petstore").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("headerA"), "headerA");
            request.getRequestHeaders().put(new HttpString("headerB"), "headerB");
            request.getRequestHeaders().put(new HttpString("keyA"), "oldA");
            request.getRequestHeaders().put(new HttpString("keyB"), "oldB");
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
        Assert.assertEquals("{\"requestHeaders\":{\"keyA\":\"valueA\",\"keyB\":\"valueB\"},\"responseHeaders\":{\"keyC\":\"valueC\",\"keyD\":\"valueD\"}}", body);
    }

    @Test
    public void testMarketHeader() throws Exception {
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
            ClientRequest request = new ClientRequest().setPath("/market").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("headerE"), "headerE");
            request.getRequestHeaders().put(new HttpString("headerF"), "headerF");
            request.getRequestHeaders().put(new HttpString("keyE"), "oldE");
            request.getRequestHeaders().put(new HttpString("keyF"), "oldF");
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
        Assert.assertEquals("{\"requestHeaders\":{\"keyE\":\"valueE\",\"keyF\":\"valueF\"},\"responseHeaders\":{\"keyG\":\"valueG\",\"keyH\":\"valueH\"}}", body);
    }

}
