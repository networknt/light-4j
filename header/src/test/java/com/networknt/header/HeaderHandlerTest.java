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
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @BeforeAll
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

    @AfterAll
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
                    responseHeaders.put("keyC", "valueC");
                    responseHeaders.put("keyD", "valueD");
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
                    responseHeaders.put("keyG", "valueG");
                    responseHeaders.put("keyH", "valueH");
                    headers.put("responseHeaders", responseHeaders);
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(headers));
                })
                .add(Methods.GET, "/extraHeaders", exchange -> {
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("extraHeader", "extraHeaderValue");
                    exchange.getResponseHeaders().put(new HttpString("extraHeader"), "extraHeader");
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(responseHeaders));
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
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
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

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assertions.assertEquals(200, statusCode);
	List<String> possibleJson = getPossibleJson("key1", "value1", "key2", "value2", "key1", "value1", "key2", "value2");
        Assertions.assertTrue(possibleJson.contains(body));
    }

    @Test
    public void testResponseHeaderRemoval() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/extraHeaders").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();

        Assertions.assertEquals(200, statusCode);

        var responseHeaders = reference.get().getResponseHeaders();
        Assertions.assertFalse(responseHeaders.contains("extraHeader"));
    }

    @Test
    public void testPetstoreHeader() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
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

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assertions.assertEquals(200, statusCode);
        var responseHeaders = reference.get().getResponseHeaders();
        Assertions.assertTrue(responseHeaders.contains("keyC"));
        Assertions.assertTrue(responseHeaders.contains("keyD"));
	List<String> possibleJson = getPossibleJson("keyA", "valueA", "keyB", "valueB", "keyC", "valueC", "keyD", "valueD");
        Assertions.assertTrue(possibleJson.contains(body));
    }

    @Test
    public void testMarketHeader() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
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

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assertions.assertEquals(200, statusCode);
	List<String> possibleJson = getPossibleJson("keyE", "valueE", "keyF", "valueF", "keyG", "valueG", "keyH", "valueH");
        Assertions.assertTrue(possibleJson.contains(body));
    }

    List<String> getPossibleJson(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4){
        List<String> possibleJson = new ArrayList<>();
        possibleJson.add("{\"requestHeaders\":{\""+key1+"\":\""+value1+"\",\""+key2+"\":\""+value2+"\"},\"responseHeaders\":{\""+key3+"\":\""+value3+"\",\""+key4+"\":\""+value4+"\"}}");
        possibleJson.add("{\"requestHeaders\":{\""+key1+"\":\""+value1+"\",\""+key2+"\":\""+value2+"\"},\"responseHeaders\":{\""+key4+"\":\""+value4+"\",\""+key3+"\":\""+value3+"\"}}");
        possibleJson.add("{\"requestHeaders\":{\""+key2+"\":\""+value2+"\",\""+key1+"\":\""+value1+"\"},\"responseHeaders\":{\""+key3+"\":\""+value3+"\",\""+key4+"\":\""+value4+"\"}}");
        possibleJson.add("{\"requestHeaders\":{\""+key2+"\":\""+value2+"\",\""+key1+"\":\""+value1+"\"},\"responseHeaders\":{\""+key4+"\":\""+value4+"\",\""+key3+"\":\""+value3+"\"}}");
        possibleJson.add("{\"responseHeaders\":{\""+key3+"\":\""+value3+"\",\""+key4+"\":\""+value4+"\"},\"requestHeaders\":{\""+key1+"\":\""+value1+"\",\""+key2+"\":\""+value2+"\"}}");
        possibleJson.add("{\"responseHeaders\":{\""+key3+"\":\""+value3+"\",\""+key4+"\":\""+value4+"\"},\"requestHeaders\":{\""+key2+"\":\""+value2+"\",\""+key1+"\":\""+value1+"\"}}");
        possibleJson.add("{\"responseHeaders\":{\""+key4+"\":\""+value4+"\",\""+key3+"\":\""+value3+"\"},\"requestHeaders\":{\""+key1+"\":\""+value1+"\",\""+key2+"\":\""+value2+"\"}}");
        possibleJson.add("{\"responseHeaders\":{\""+key4+"\":\""+value4+"\",\""+key3+"\":\""+value3+"\"},\"requestHeaders\":{\""+key2+"\":\""+value2+"\",\""+key1+"\":\""+value1+"\"}}");
        return possibleJson;
    }

}
