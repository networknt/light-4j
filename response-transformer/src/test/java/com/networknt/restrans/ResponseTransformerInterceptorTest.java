package com.networknt.restrans;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.exception.ClientException;
import com.networknt.handler.ResponseInterceptorInjectionHandler;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Disabled
public class ResponseTransformerInterceptorTest {
    static final Logger logger = LoggerFactory.getLogger(ResponseTransformerInterceptorTest.class);

    static Undertow server = null;

    @BeforeAll
    public static void setUp() throws Exception {
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            ResponseInterceptorInjectionHandler sinkHandler = new ResponseInterceptorInjectionHandler();
            sinkHandler.setNext(handler);
            handler = sinkHandler;
            server = Undertow.builder()
                    .addHttpListener(7080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
            RuleLoaderStartupHook startupHook = (RuleLoaderStartupHook) SingletonServiceFactory.getBean(StartupHookProvider.class);
            startupHook.onStartup();
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
                .add(Methods.GET, "/v1/pets", exchange -> exchange.getResponseSender().send("{\"data\":null,\"notifications\":[{\"code\":\"ERR00610000\",\"message\":\"Exception in getting service:Unable to create user info\",\"timestamp\":1655739885937,\"metadata\":null,\"description\":\"Internal Server Error\"}]}"))
                .add(Methods.POST, "/post", exchange -> exchange.getResponseSender().send("post"));
    }

    @Test
    @Disabled
    public void testGetRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/v1/pets").setMethod(Methods.GET);
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
        if(statusCode == 200) {
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assertions.assertNotNull(body);
            Assertions.assertEquals("[{\"com.networknt.handler.ResponseInterceptorHandler\":[\"com.networknt.restrans.ResponseTransformerHandler\"]}]", body);
        }
    }

    @Test
    @Disabled
    public void testPostRequest() throws Exception {
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();

        try {
            String post = "post";
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/post");
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

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        Assertions.assertEquals(200, statusCode);
        if(statusCode == 200) {
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assertions.assertNotNull(body);
            Assertions.assertEquals("[{\"com.networknt.handler.ResponseInterceptorHandler\":[\"com.networknt.restrans.ResponseTransformerHandler\"]}]", body);
        }
    }


    @Test
    public void testMatch() {
        List<String> list = new ArrayList<>();
        list.add("/corp/mras/1.0.0");
        list.add("/corp/lab/1.0");
        list.add("/gateway/partyInfo/1.0");
        String requestPath = "/gateway/partyInfo/1.0/salesforce";

        Optional<String> match = findMatchingPrefix(requestPath, list);
        Assertions.assertTrue(match.isPresent());
        Assertions.assertEquals("/gateway/partyInfo/1.0", match.get());

        requestPath = "/corp/lab/1.0";
        match = findMatchingPrefix(requestPath, list);
        Assertions.assertTrue(match.isPresent());
        Assertions.assertEquals("/corp/lab/1.0", match.get());

    }

    Optional<String> findMatchingPrefix(String url, List<String> prefixes) {
        return prefixes.stream()
                .filter(url::startsWith)
                .findFirst();
    }

}
