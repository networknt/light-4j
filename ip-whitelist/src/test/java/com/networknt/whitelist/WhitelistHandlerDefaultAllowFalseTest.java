package com.networknt.whitelist;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.exception.ClientException;
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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This test is using the whitelist-map.yml file with the defaultAllow is false. For each test case, there are comment
 * about the use case and the expected result to show users what how this flag works.
 *
 * From logical perspective, this setup is more natural and easy to understand; however, as all the request paths
 * that are not defined will be allowed to access, this might not be the desired behaviour for some organizations.
 *
 * The defaultAllow is default to true in the built-in whitelist.yml, so this is the default behaviour.
 */
public class WhitelistHandlerDefaultAllowFalseTest {
    static final Logger logger = LoggerFactory.getLogger(WhitelistHandlerDefaultAllowFalseTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if (server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            WhitelistHandler whitelistHandler = new WhitelistHandler("whitelist-map");
            if(whitelistHandler.isEnabled()) {
                whitelistHandler.setNext(handler);
                handler = whitelistHandler;
            }
            server = Undertow.builder()
                    .addHttpListener(7080, "localhost")
                    .setHandler(handler)
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
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
                .add(Methods.GET, "/data", exchange -> {
                    exchange.getResponseSender().send("OK");
                })
                .add(Methods.GET, "/default", exchange -> {
                    exchange.getResponseSender().send("OK");
                })
                .add(Methods.GET, "/data/extra", exchange -> {
                    exchange.getResponseSender().send("OK");
                })
                .add(Methods.GET, "/health/com.networknt.petstore-1.0.0", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    /**
     * In this test case, we send a request exactly as defined in the paths and 127.0.0.1 is in the rule list
     * for this prefix. We should have 403 error response as default allow is false for the IP and request path
     * matches the prefix in the config. When defaultAllow is false, all the IP defined for a path prefix should
     * be rejected. Basically, all IP addresses defined will be denied given defaultAllow is false.
     *
     * @throws Exception
     */
    @Test
    public void testWhitelistPathIpDefined() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/health/com.networknt.petstore-1.0.0").setMethod(Methods.GET);
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
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(403, statusCode);
        if (statusCode == 403) {
            Assert.assertTrue(body.contains("ERR10049"));
        }
    }

    /**
     * In this test case, we use the exact request path as the path prefix defined in the config /data; however, the
     * 127.0.0.1 IP address is not defined in the rule list. As the defaultAllow is false and the IP is not in the rule,
     * the request will be allowed and 200 is returned. This means if the path is matched, the IP must not be in the rule
     * to allow the access. Any incoming IP that is not in the list definition for the path prefix will be allowed.
     *
     * @throws Exception
     */
    @Test
    public void testExactPathIpNotDefined() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/data").setMethod(Methods.GET);
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
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        if (statusCode == 200) {
            Assert.assertTrue(body.contains("OK"));
        }
    }

    /**
     * In this test case, we use a long request url /data/extra and the configuration has the path prefix /data defined.
     * As 127.0.0.1 IP address is not defined in the rule list. As the defaultAllow is false and the IP is not in the rule,
     * the request will be allowed and 200 is returned. This means if the path is matched, the IP must not be in the rule
     * to allow the access. Any incoming IP that is not in the list definition for the path prefix will be allowed.
     *
     * @throws Exception
     */
    @Test
    public void testPathPrefixIpNotDefined() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/data/extra").setMethod(Methods.GET);
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
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        if (statusCode == 200) {
            Assert.assertTrue(body.contains("OK"));
        }
    }


    /**
     * In this test case, we are using a path that is not defined in the configuration. As the defaultAllow is false,
     * this request will be rejected in the whitelist check and return 403 code. This means all paths that are not defined
     * in the whitelist.paths will be rejected if defaultAllow is false.
     * @throws Exception
     */
    @Test
    public void testPathNotDefined() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/default").setMethod(Methods.GET);
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
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(403, statusCode);
        if (statusCode == 403) {
            Assert.assertTrue(body.contains("ERR10049"));
        }
    }

}
