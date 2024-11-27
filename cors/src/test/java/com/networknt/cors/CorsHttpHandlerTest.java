/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

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
import io.undertow.server.HttpServerExchange;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.cors.CorsHeaders.*;
import static com.networknt.cors.CorsHttpHandler.isCorsRequest;
import static com.networknt.cors.CorsHttpHandler.matchOrigin;
import static io.undertow.util.Headers.HOST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
                })
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testOptionsWrongOrigin() throws Exception {
        String url = "http://localhost:7080";
        Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
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
    public void testOptionsLocalhostOrigin() throws Exception {
        String url = "http://localhost:7080";
        Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
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

    @Test
    public void testOptionsCorrectOrigin() throws Exception {
        String url = "http://localhost:7080";
        Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/").setMethod(Methods.OPTIONS);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("Origin"), "https://www.xyz.com");
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

    /**
     * Test of isCoreRequest method, of class CorsUtil.
     */
    @Test
    public void testIsCorsRequest() {
        HeaderMap headers = new HeaderMap();
        assertThat(isCorsRequest(headers), is(false));
        headers = new HeaderMap();
        headers.add(new HttpString(ORIGIN), "");
        assertThat(isCorsRequest(headers), is(true));
        headers = new HeaderMap();
        headers.add(new HttpString(ACCESS_CONTROL_REQUEST_HEADERS), "");
        assertThat(isCorsRequest(headers), is(true));
        headers = new HeaderMap();
        headers.add(new HttpString(ACCESS_CONTROL_REQUEST_METHOD), "");
        assertThat(isCorsRequest(headers), is(true));
    }

    /**
     * Test of matchOrigin method, of class CorsUtil.
     */
    @Test
    public void testMatchOrigin() throws Exception {
        HeaderMap headerMap = new HeaderMap();
        headerMap.add(HOST, "localhost:80");
        headerMap.add(new HttpString(ORIGIN), "http://localhost");
        HttpServerExchange exchange = new HttpServerExchange(null, headerMap, new HeaderMap(), 10);
        exchange.setRequestScheme("http");
        exchange.setRequestMethod(HttpString.EMPTY);
        Collection<String> allowedOrigins = null;
        assertThat(matchOrigin(exchange, allowedOrigins), is("http://localhost"));
        allowedOrigins = Collections.singletonList("http://www.example.com:9990");
        //Default origin
        assertThat(matchOrigin(exchange, allowedOrigins), is("http://localhost"));
        headerMap.clear();
        headerMap.add(HOST, "localhost:80");
        headerMap.add(new HttpString(ORIGIN), "http://www.example.com:9990");
        assertThat(matchOrigin(exchange, allowedOrigins), is("http://www.example.com:9990"));
        headerMap.clear();
        headerMap.add(HOST, "localhost:80");
        headerMap.add(new HttpString(ORIGIN), "http://www.example.com");
        assertThat(matchOrigin(exchange, allowedOrigins), is(nullValue()));
        headerMap.addAll(new HttpString(ORIGIN), Arrays.asList("http://localhost:7080", "http://www.example.com:9990", "http://localhost"));
        allowedOrigins = Arrays.asList("http://localhost", "http://www.example.com:9990");
        assertThat(matchOrigin(exchange, allowedOrigins), is("http://localhost"));
    }


}
