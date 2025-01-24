package com.networknt.token.limit;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.exception.ClientException;
import com.networknt.server.ServerConfig;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Ignore
public class TokenLimitHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(TokenLimitHandlerTest.class);
    @ClassRule
    public static TestServer server = TestServer.getInstance();
    static final boolean enableHttp2 = ServerConfig.getInstance().isEnableHttp2();
    static final boolean enableHttps = ServerConfig.getInstance().isEnableHttps();
    static final int httpPort = ServerConfig.getInstance().getHttpPort();
    static final int httpsPort = ServerConfig.getInstance().getHttpsPort();
    static final String url = enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final String JSON_MEDIA_TYPE = "application/json";
    final Http2Client client = Http2Client.getInstance();

    //private static TokenLimitConfig config;
    String legacyRequestBody = "grant_type=client_credentials&client_id=legacyClient&client_secret=secret&scope=scope";
    String nonLegacyRequestBody = "grant_type=client_credentials&client_id=NonlegacyClient&client_secret=secret&scope=scope";

    public String callLegacyClient() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionHolder.ConnectionToken connectionToken = null;
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            if(enableHttps) {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);
            } else {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            }
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();

            ClientRequest request = new ClientRequest().setPath("/oauth2/1234123/v1/token").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, legacyRequestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(connectionToken);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY) + ":" + reference.get().getResponseCode();
    }

    public String callNonLegacyClient() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionHolder.ConnectionToken connectionToken = null;
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();

        try {
            if(enableHttps) {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);
            } else {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            }
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();

            ClientRequest request = new ClientRequest().setPath("/oauth2/1234123/v1/token").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, nonLegacyRequestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(connectionToken);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY) + ":" + reference.get().getResponseCode();
    }

    @Test
    public void testOneRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionHolder.ConnectionToken connectionToken = null;
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            if(enableHttps) {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);
            } else {
                connectionToken = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            }
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            ClientRequest request = new ClientRequest().setPath("/oauth2/1234123/v1/token").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, nonLegacyRequestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(connectionToken);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        if(statusCode == 200) {
            Assert.assertEquals("{\"accessToken\":\"abc\",\"counter\": 0}", body);
        }
    }

    /**
     * For non-legacy client, the token limit should be applied. And we should have at least one 400 response.
     * @throws Exception exception
     */
    @Test
    public void testHandleRequest_NonLegacyClient() throws Exception {
        Callable<String> task = this::callNonLegacyClient;
        List<Callable<String>> tasks = Collections.nCopies(3, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            logger.info("calling");
            String s = future.get();
            logger.info("future = " + s);
            resultList.add(s);
        }
        long last = (System.currentTimeMillis() - start);
        // make sure that there are at least one element is 400
        List<String> errorList = resultList.stream().filter(r->r.contains(":400")).collect(Collectors.toList());
        logger.info("errorList size = " + errorList.size());
        Assert.assertTrue(errorList.size()>0);
    }

    /**
     * For legacy client, the response is cached so multiple request will have the same response from the token limit
     * handler with the same counter in the body. However, due to multi-threading, two or more requests might be handled
     * at the same time and the cache might be not ready or updated for some requests. That is why five requests will be
     * sent here with only two threads available in the pool, and we only want to count at least two of them are the same
     * with same counter.
     *
     * @throws Exception exception
     */
    @Test
    public void testHandleRequest_LegacyClient() throws Exception {
        Callable<String> task = this::callLegacyClient;
        List<Callable<String>> tasks = Collections.nCopies(5, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            String s = future.get();
            resultList.add(s);
        }
        long last = (System.currentTimeMillis() - start);
        // make sure at least there are some duplicated entries.
        Assert.assertTrue(hasDuplicates(resultList));
    }

    public static boolean hasDuplicates(List<String> list) {
        HashSet<String> set = new HashSet<>();
        for (String str : list) {
            if (!set.add(str)) {
                return true; // Duplicate found
            }
        }
        return false; // No duplicates
    }
}
