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

package com.networknt.limit;

import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Created by steve on 23/09/16.
 */
public class LimitHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(LimitHandlerTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            LimitHandler limitHandler = new LimitHandler();
            limitHandler.setNext(handler);
            handler = limitHandler;
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
                .add(Methods.GET, "/", exchange -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {

                    }
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testOneRequest() throws Exception {
        String url = "http://localhost:8080";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = IOUtils.toString(response.getEntity().getContent(), "utf8");
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                Assert.assertNotNull(body);
                Assert.assertEquals("OK", body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String callApi() throws Exception {
        String url = "http://localhost:8080";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = response -> {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = IOUtils.toString(response.getEntity().getContent(), "utf8");
            return body + ":" + statusCode;
        };
        String response = "";
        try {
            response = client.execute(httpGet, responseHandler);
            logger.debug("response = " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    @Test
    public void testMoreRequests() throws Exception {
        Callable<String> task = this::callApi;
        List<Callable<String>> tasks = Collections.nCopies(10, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            resultList.add(future.get());
        }
        long last = (System.currentTimeMillis() - start);
        // make sure that there are at least one element in resultList is :513
        Assert.assertTrue(resultList.contains(":513"));
        System.out.println("resultList = " + resultList + " response time = " + last);
    }
}
