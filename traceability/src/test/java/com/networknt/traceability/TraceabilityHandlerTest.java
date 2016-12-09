/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.traceability;

import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by steve on 23/09/16.
 */
public class TraceabilityHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(TraceabilityHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            TraceabilityHandler traceabilityHandler = new TraceabilityHandler();
            traceabilityHandler.setNext(handler);
            handler = traceabilityHandler;
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
                .add(Methods.GET, "/get", exchange -> exchange.getResponseSender().send("get"))
                .add(Methods.POST, "/post", exchange -> exchange.getResponseSender().send("post"));
    }

    @Test
    public void testGetWithTid() throws Exception {
        String url = "http://localhost:8080/get";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(Constants.TRACEABILITY_ID, "12345");
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("get", s);
                Header tid = response.getFirstHeader(Constants.TRACEABILITY_ID);
                Assert.assertEquals("12345", tid.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetWithoutTid() throws Exception {
        String url = "http://localhost:8080/get";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("get", s);
                Header tid = response.getFirstHeader(Constants.TRACEABILITY_ID);
                Assert.assertNull(tid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPostWithTid() throws Exception {
        String url = "http://localhost:8080/post";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(Constants.TRACEABILITY_ID, "12345");
        httpPost.setHeader(Headers.CONTENT_TYPE.toString(), "application/json");
        try {
            StringEntity stringEntity = new StringEntity("post");
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("post", s);
                Header tid = response.getFirstHeader(Constants.TRACEABILITY_ID);
                Assert.assertEquals("12345", tid.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
