package com.networknt.router.middleware;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.AuthServerConfig;
import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the test class to ensure that the TokenHandler is working with multiple
 * OAuth 2.0 providers with proper cache based on the serviceId and scopes.
 *
 * @author Steve Hu
 */
public class TokenHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(TokenHandlerTest.class);
    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception{
        if(server == null) {
            logger.info("starting server");
            HttpHandler handler = getTestHandler();
            TokenHandler tokenHandler = new TokenHandler();
            tokenHandler.setNext(handler);
            handler = tokenHandler;
            server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(7080, "localhost")
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
                .add(Methods.POST, "/", exchange -> {
                    exchange.getResponseSender().send("POST OK");
                })
                .add(Methods.GET, "/", exchange -> {
                    exchange.getResponseSender().send("GET OK");
                });
    }

    @Test
    @Ignore
    public void testOneGetService1Request() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionHolder.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/service1").setMethod(Methods.GET);
            request.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, "service1");
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
        if(statusCode == 200) {
            Assert.assertEquals("GET OK", body);
        }
    }

    @Test
    @Ignore
    public void testOnePostRequest() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionHolder.ConnectionToken token;

        try {

            token = client.borrow(new URI("http://localhost:7080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "{\"clientId\":\"FSC_0030303343303x32AA2\",\"loggedInUserEmail\":\"steve.hu@networknt.com\"}";
        try {
            ClientRequest request = new ClientRequest().setPath("/services/apexrest/NNT_ConquestApplicationServices/getAccountDetailsForConquestToUpdatePlan").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("statusCode = " + statusCode + " body = " + body);
        Assert.assertEquals(200, statusCode);
    }

    @Test
    public void testGetJwtToken_Success() {
        TokenHandler.cache.clear();
        String serviceId = "service1";
        long currentTime = System.currentTimeMillis();

        // Mock cached JWT
        Jwt cachedJwt = new Jwt(new Jwt.Key(serviceId));
        cachedJwt.setExpire(currentTime + 60000); // Token expires in 60 seconds
        TokenHandler.cache.put(serviceId, cachedJwt);

        // Call the method
        Result<Jwt> result = TokenHandler.getJwtToken(serviceId);

        // Verify the result
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(cachedJwt, result.getResult());
    }

    @Test
    public void testGetJwtToken_Failure() {
        TokenHandler.cache.clear();
        String serviceId = "service1";

        // Call the method
        Result<Jwt> result = TokenHandler.getJwtToken(serviceId);

        // Verify the result
        Assert.assertTrue(result.isFailure());
        Assert.assertNull(TokenHandler.cache.get(serviceId)); // Verify cache is not updated
    }

    @Test
    public void testBuildConfigMap() {
        final var config = ClientConfig.get().getOAuth().getToken().getClientCredentials().getServiceIdAuthServers();
        Assert.assertNotNull(config);

        final AuthServerConfig serviceConfig = config.get("service1");
        Assert.assertNotNull(serviceConfig);

        final var mapper = Config.getInstance().getMapper();
        final var configMap = mapper.convertValue(serviceConfig, new TypeReference<Map<String, Object>>() {
        });
        Assert.assertNotNull(configMap);

        final var completeAuthConfig = TokenHandler.enrichAuthServerConfig(serviceConfig, ClientConfig.get().getOAuth().getToken());
        Assert.assertNotNull(completeAuthConfig);

        // config should not contain proxy settings
        Assert.assertNull(completeAuthConfig.getProxyHost());
    }

}
