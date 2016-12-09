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

package com.networknt.exception;

import com.networknt.config.Config;
import com.networknt.status.Status;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Scanner;

/**
 * Created by steve on 23/09/16.
 */
public class ExceptionHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            ExceptionHandler exceptionHandler = new ExceptionHandler();
            exceptionHandler.setNext(handler);
            handler = exceptionHandler;
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

    @SuppressWarnings("ConstantOverflow")
    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/normal", exchange -> exchange.getResponseSender().send("normal"))
                .add(Methods.GET, "/runtime", exchange -> {
                    int i = 1/0;
                })
                .add(Methods.GET, "/api", exchange -> {
                    Status error = new Status("ERR10001");
                    throw new ApiException(error);
                })
                .add(Methods.GET, "/uncaught", exchange -> {
                    String content = new Scanner(new File("djfkjoiwejjhh9032d")).useDelimiter("\\Z").next();
                });
    }

    @Test
    public void testNormal() throws Exception {
        String url = "http://localhost:8080/normal";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                String s = IOUtils.toString(response.getEntity().getContent(), "utf8");
                Assert.assertNotNull(s);
                Assert.assertEquals("normal", s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRuntimeException() throws Exception {
        String url = "http://localhost:8080/runtime";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(500, statusCode);
            if(statusCode == 500) {
                Status status = Config.getInstance().getMapper().readValue(response.getEntity().getContent(), Status.class);
                Assert.assertNotNull(status);
                Assert.assertEquals("ERR10010", status.getCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testApiException() throws Exception {
        String url = "http://localhost:8080/api";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            Assert.assertEquals(401, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUncaughtException() throws Exception {
        String url = "http://localhost:8080/uncaught";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            Assert.assertEquals(400, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
