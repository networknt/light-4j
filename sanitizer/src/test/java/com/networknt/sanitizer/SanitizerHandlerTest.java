package com.networknt.sanitizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.body.BodyHandler;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.*;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by steve on 22/10/16.
 */
public class SanitizerHandlerTest {

    static final Logger logger = LoggerFactory.getLogger(SanitizerHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();

            SanitizerHandler sanitizerHandler = new SanitizerHandler();
            sanitizerHandler.setNext(handler);
            handler = sanitizerHandler;

            BodyHandler bodyHandler = new BodyHandler();
            bodyHandler.setNext(handler);
            handler = bodyHandler;

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
                .add(Methods.GET, "/parameter", exchange -> {
                    Map<String, Deque<String>> parameter = exchange.getQueryParameters();
                    if(parameter != null) {
                        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(parameter));
                    }
                })
                .add(Methods.GET, "/header", exchange -> {
                    HeaderMap headerMap = exchange.getRequestHeaders();
                    if(headerMap != null) {
                        exchange.getResponseSender().send(headerMap.toString());
                    }
                })
                .add(Methods.POST, "/body", exchange -> {
                    Object body = exchange.getAttachment(BodyHandler.REQUEST_BODY);
                    if(body != null) {
                        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(body));
                    }
                })
                .add(Methods.POST, "/header", exchange -> {
                    HeaderMap headerMap = exchange.getRequestHeaders();
                    if(headerMap != null) {
                        exchange.getResponseSender().send(headerMap.toString());
                    }
                });
    }

    @Test
    public void testGetHeader() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/header").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString("param"), "<script>alert('header test')</script>");
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
        Assert.assertTrue(body.contains("<script>alert(\\'header test\\')</script>"));
    }

    @Test
    public void testPostHeader() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String post = "{\"key\":\"value\"}";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/header");
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    request.getRequestHeaders().put(new HttpString("param"), "<script>alert('header test')</script>");
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
            Assert.assertTrue(body.contains("<script>alert(\\'header test\\')</script>"));
        }
    }

    @Test
    public void testPostBody() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String post = "{\"key\":\"<script>alert('test')</script>\"}";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/body");
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
            Assert.assertNotNull(body);
            Map map = Config.getInstance().getMapper().readValue(body, new TypeReference<HashMap<String, Object>>() {});
            Assert.assertEquals("<script>alert(\\'test\\')</script>", map.get("key"));
        }
    }

    @Test
    public void testEncodeNode() throws Exception {
        String data = "{\"s1\":\"<script>alert('test1')</script>\",\"s2\":[\"abc\",\"<script>alert('test2')</script>\"],\"s3\":{\"s4\":\"def\",\"s5\":\"<script>alert('test5')</script>\"},\"s6\":[{\"s7\":\"<script>alert('test7')</script>\"},{\"s8\":\"ghi\"}],\"s9\":[[\"<script>alert('test9')</script>\"],[\"jkl\"]]}";
        HashMap<String, Object> jsonMap = Config.getInstance().getMapper().readValue(data,new TypeReference<HashMap<String, Object>>(){});
        SanitizerHandler handler = new SanitizerHandler();
        handler.encodeNode(jsonMap);
        Assert.assertEquals(jsonMap.get("s1"), "<script>alert(\\'test1\\')</script>");
        ArrayList l2 = (ArrayList)jsonMap.get("s2");
        String s2 = (String)l2.get(1);
        Assert.assertEquals(s2, "<script>alert(\\'test2\\')</script>");
        HashMap<String, Object> m3 = (HashMap<String,Object>)jsonMap.get("s3");
        String s5 = (String)m3.get("s5");
        Assert.assertEquals(s5, "<script>alert(\\'test5\\')</script>");
        ArrayList l6 = (ArrayList)jsonMap.get("s6");
        HashMap<String,Object> m7 = (HashMap<String, Object>)l6.get(0);
        String s7 = (String)m7.get("s7");
        Assert.assertEquals(s7, "<script>alert(\\'test7\\')</script>");
        ArrayList l9 = (ArrayList)jsonMap.get("s9");
        ArrayList l = (ArrayList)l9.get(0);
        String s9 = (String)l.get(0);
        Assert.assertEquals(s9, "<script>alert(\\'test9\\')</script>");
    }

    @Test
    public void testEncoder() throws Exception {
        String s1 = "keep-alive";
        String s2 = "text/html";
        Assert.assertEquals(Encode.forJavaScriptSource(s1), s1);
        Assert.assertEquals(Encode.forJavaScriptSource(s2), s2);
        //Assert.assertEquals(Encode.forJavaScriptBlock(s1), s1);
        //Assert.assertEquals(Encode.forJavaScriptBlock(s2), s2);

        String s3 = "<script>alert('test')</script>";
        String e3 = Encode.forJavaScriptSource(s3);
        System.out.println("source = " + e3);
        String e4 = Encode.forJavaScriptAttribute(s3);
        System.out.println("attribute = " + e4);
        String e5 = Encode.forJavaScriptBlock(s3);
        System.out.println("block = " + e5);
        String e6 = Encode.forJavaScript(e3);
        System.out.println("script = " + e6);
        Assert.assertNotEquals(e3, s3);

        String s7 = "<script>location.href=\"respources.html\"</script>";
        String e7 = Encode.forJavaScriptSource(s7);
        System.out.println("e7 = " + e7);
    }

}
