package com.networknt.client;

import com.networknt.exception.ClientException;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import com.networknt.client.simplepool.SimpleConnectionState;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Ignore
public class HttpRequestSSLContextTest {

    static final Logger logger = LoggerFactory.getLogger(HttpRequestSSLContextTest.class);
    public static final String CONFIG_NAME = "client";
    static ClientConfig config;

    @BeforeClass
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    @Test (expected = NullPointerException.class)
    public void testGetRequestSSlContextError() throws Exception{

        Map<String, Object>  tlsConfig = config.getTlsConfig();
        tlsConfig.put("verifyHostname", false);

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionState.ConnectionToken token;
        final ClientConnection connection;
        try {
            token = client.borrow(new URI("https://www.google.com"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
            connection = (ClientConnection) token.getRawConnection();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestUri = "/";
        try {
            ClientRequest request = new ClientRequest().setPath(requestUri).setMethod(Methods.GET);

            //customized header parameters
            request.getRequestHeaders().put(new HttpString("host"), "www.google.com");
            connection.sendRequest(request, client.createClientCallback(reference, latch));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(token);
        }
        int statusCode = reference.get().getResponseCode();
    }

    @Test
    public void testGetRequest() throws Exception{

        Map<String, Object>  tlsConfig = config.getTlsConfig();
        tlsConfig.put("loadDefaultTrustStore", true);
        tlsConfig.put("verifyHostname", false);

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionState.ConnectionToken token;
        final ClientConnection connection;
        try {
            token = client.borrow(new URI("https://www.google.com"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
            connection = (ClientConnection) token.getRawConnection();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestUri = "/";
        try {
            ClientRequest request = new ClientRequest().setPath(requestUri).setMethod(Methods.GET);

            request.getRequestHeaders().put(new HttpString("host"), "www.google.com");
            connection.sendRequest(request, client.createClientCallback(reference, latch));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(token);
        }
        int statusCode = reference.get().getResponseCode();
        Assert.assertEquals(200, statusCode);
    }

}
