package com.networknt.sanitizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
            } catch (InterruptedException ie) {

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

    /*
    @Test
    public void testGetParameter() throws Exception {
        String url = "http://localhost:8080/parameter?p=<script>alert('This is a test')</script>";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("nobody", s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */

    @Test
    public void testGetHeader() throws Exception {
        String url = "http://localhost:8080/header";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("param", "<script>alert('header test')</script>");
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertTrue(s.contains("<script>alert(\\'header test\\')</script>"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPostHeader() throws Exception {
        String url = "http://localhost:8080/header";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");
        httpPost.setHeader("param", "<script>alert('header test')</script>");
        try {
            StringEntity stringEntity = new StringEntity("{\"key\":\"value\"}");
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertTrue(s.contains("<script>alert(\\'header test\\')</script>"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPostBody() throws Exception {
        String url = "http://localhost:8080/body";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");

        try {
            StringEntity stringEntity = new StringEntity("{\"key\":\"<script>alert('test')</script>\"}");
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Map map = Config.getInstance().getMapper().readValue(s, new TypeReference<HashMap<String, Object>>() {});
                Assert.assertEquals("<script>alert(\\'test\\')</script>", map.get("key"));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
