package com.networknt.cors;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
                    exchange.getResponseSender().send("OK");
                })
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testOptionsWrongOrigin() throws Exception {
        String url = "http://localhost:8080";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpOptions httpOptions = new HttpOptions(url);
        httpOptions.setHeader("Origin", "http://example.com");
        httpOptions.setHeader("Access-Control-Request-Method", "POST");
        httpOptions.setHeader("Access-Control-Request-Headers", "X-Requested-With");

        try {
            CloseableHttpResponse response = client.execute(httpOptions);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = IOUtils.toString(response.getEntity().getContent(), "utf8");
            Header header = response.getFirstHeader("Access-Control-Allow-Origin");
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                Assert.assertNull(header);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOptionsCorrectOrigin() throws Exception {
        String url = "http://localhost:8080";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpOptions httpOptions = new HttpOptions(url);
        httpOptions.setHeader("Origin", "http://localhost");
        httpOptions.setHeader("Access-Control-Request-Method", "POST");
        httpOptions.setHeader("Access-Control-Request-Headers", "X-Requested-With");

        try {
            CloseableHttpResponse response = client.execute(httpOptions);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = IOUtils.toString(response.getEntity().getContent(), "utf8");
            Header header = response.getFirstHeader("Access-Control-Allow-Origin");
            Assert.assertEquals(200, statusCode);
            if(statusCode == 200) {
                Assert.assertNotNull(header);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
