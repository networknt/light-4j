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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.omg.SendingContext.RunTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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
            ExceptionHandler exceptionHandler = new ExceptionHandler(handler);
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
            } catch (InterruptedException ie) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
    }

    static RoutingHandler getTestHandler() {
        RoutingHandler handler = Handlers.routing()
                .add(Methods.GET, "/runtime", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        int i = 1/0;
                    }
                })
                .add(Methods.GET, "/api", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        Status error = new Status("ERR10001");
                        throw new ApiException(error);
                    }
                })
                .add(Methods.GET, "/uncaught", new HttpHandler() {
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        String content = new Scanner(new File("djfkjoiwejjhh9032d")).useDelimiter("\\Z").next();
                    }
                });
        return handler;
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
