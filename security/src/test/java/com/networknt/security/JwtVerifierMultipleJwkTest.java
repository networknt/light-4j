package com.networknt.security;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.jose4j.jwt.JwtClaims;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class JwtVerifierMultipleJwkTest extends JwtVerifierJwkBase {
    private static final Logger logger = LoggerFactory.getLogger(JwtVerifierMultipleJwkTest.class);
    private static final String SERVER_KEY_STORE = "server.keystore";
    private static final String SERVER_TRUST_STORE = "server.truststore";

    public static final String TOKEN = "/oauth2/token";
    public static final String API_PETSTORE = "/api/petstore";
    public static final String API_MARKET = "/api/market";
    public static final String KEY = "/oauth2/key";
    static ClientConfig config;
    static SecurityConfig securityConfig;

    private static XnioWorker worker;
    static Undertow server1 = null;
    static Undertow server2 = null;
    static Undertow server3 = null;
    static Undertow server4 = null;

    static SSLContext sslContext;

    @BeforeClass
    public static void beforeClass() throws IOException {
        securityConfig = SecurityConfig.load(JwtVerifier.SECURITY_CONFIG);
        config = ClientConfig.get();
        // Create xnio worker
        final Xnio xnio = Xnio.getInstance();
        final XnioWorker xnioWorker = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
        worker = xnioWorker;

        if(server1 == null) {
            System.err.println("starting server1");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            // as we are testing OAuth client, only https/2 is used.
            builder.addHttpsListener(7771, "localhost", sslContext);
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            server1 = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors())
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(API_PETSTORE, (exchange) -> {
                                boolean hasScopeToken = exchange.getRequestHeaders().contains(HttpStringConstants.SCOPE_TOKEN);
                                String requestPath = exchange.getRequestPath();
                                Assert.assertTrue(hasScopeToken);
                                String scopeToken = exchange.getRequestHeaders().get(HttpStringConstants.SCOPE_TOKEN, 0).substring(7);
                                // verify the jwt token with JWK.
                                JwtVerifier jwtVerifier = new JwtVerifier(securityConfig);
                                JwtClaims claims = jwtVerifier.verifyJwt(scopeToken, true, true, null, requestPath, null);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(ByteBuffer.wrap(
                                        Config.getInstance().getMapper().writeValueAsBytes(
                                                Collections.singletonMap("message", "Petstore OK!"))));

                            }))
                    .setWorkerThreads(20)
                    .build();

            server1.start();
        }

        if(server2 == null) {
            System.err.println("starting server2");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            // as we are testing OAuth client, only https/2 is used.
            builder.addHttpsListener(7772, "localhost", sslContext);
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            server2 = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors())
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(API_MARKET, (exchange) -> {
                                boolean hasScopeToken = exchange.getRequestHeaders().contains(HttpStringConstants.SCOPE_TOKEN);
                                String requestPath = exchange.getRequestPath();
                                Assert.assertTrue(hasScopeToken);
                                String scopeToken = exchange.getRequestHeaders().get(HttpStringConstants.SCOPE_TOKEN, 0).substring(7);
                                // verify the jwt token with JWK.
                                JwtVerifier jwtVerifier = new JwtVerifier(securityConfig);
                                JwtClaims claims = jwtVerifier.verifyJwt(scopeToken, true, true, null, requestPath, null);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(ByteBuffer.wrap(
                                        Config.getInstance().getMapper().writeValueAsBytes(
                                                Collections.singletonMap("message", "Market OK!"))));

                            }))
                    .setWorkerThreads(20)
                    .build();

            server2.start();
        }

        if(server3 == null) {
            System.err.println("starting server3");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            // as we are testing OAuth client, only https/2 is used.
            builder.addHttpsListener(7773, "localhost", sslContext);
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            server3 = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors())
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(KEY, exchange -> {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(jsonWebKeySetJsonCurr);
                            })
                            .addExactPath(TOKEN, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    try {
                                        Map<String, Object> map = new HashMap<>();
                                        String token = getJwt(5, "7pGHLozGRXqv2g47T1HQag");
                                        map.put("access_token", token);
                                        map.put("token_type", "Bearer");
                                        map.put("expires_in", 5);
                                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                        exchange.getResponseSender().send(ByteBuffer.wrap(
                                                Config.getInstance().getMapper().writeValueAsBytes(map)));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            })))
                    .setWorkerThreads(20)
                    .build();

            server3.start();
        }

        if(server4 == null) {
            System.err.println("starting server4");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            // as we are testing OAuth client, only https/2 is used.
            builder.addHttpsListener(7774, "localhost", sslContext);
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            server4 = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors())
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(KEY, exchange -> {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(jsonWebKeySetJsonLong);
                            })
                            .addExactPath(TOKEN, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    try {
                                        Map<String, Object> map = new HashMap<>();
                                        String token = getJwt(5, "Tj_l_tIBTginOtQbL0Pv5w");
                                        map.put("access_token", token);
                                        map.put("token_type", "Bearer");
                                        map.put("expires_in", 5);
                                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                        exchange.getResponseSender().send(ByteBuffer.wrap(
                                                Config.getInstance().getMapper().writeValueAsBytes(map)));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            })))
                    .setWorkerThreads(20)
                    .build();

            server4.start();
        }

    }

    @AfterClass
    public static void afterClass() {
        worker.shutdown();
        if(server1 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server1.stop();
            server1 = null;
            System.err.println("The server1 is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if(server2 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server2.stop();
            server2 = null;
            System.err.println("The server2 is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if(server3 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server3.stop();
            server3 = null;
            System.err.println("The server3 is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        if(server4 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server4.stop();
            server4 = null;
            System.err.println("The server4 is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

    }

    private String callPetstoreApiAsync() throws Exception {
        final Http2Client client = createClient();
        // get a connection from the connection pool.
        final ClientConnection connection = client.borrowConnection(new URI("https://localhost:7771"), worker, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(API_PETSTORE).setMethod(Methods.GET);
            // this will force the client to get a scope token from the auth server 1 and put into the X-Scope-Token header.
            client.populateHeader(request, "Bearer token", "cid", "tid");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            final ClientResponse response = reference.get();
            Assert.assertEquals("{\"message\":\"Petstore OK!\"}", response.getAttachment(Http2Client.RESPONSE_BODY));
        } finally {
            // return the connection to the connection pool.
            client.returnConnection(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
    }

    @Test
    public void testSinglePetstoreAsych() throws Exception {
        callPetstoreApiAsync();
    }

    private String callMarketApiAsync() throws Exception {
        final Http2Client client = createClient();
        // get a connection from the connection pool.
        final ClientConnection connection = client.borrowConnection(new URI("https://localhost:7772"), worker, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(API_MARKET).setMethod(Methods.GET);
            // this will force the client to get a scope token from the auth server 1 and put into the X-Scope-Token header.
            client.populateHeader(request, "Bearer token", "cid", "tid");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            final ClientResponse response = reference.get();
            Assert.assertEquals("{\"message\":\"Market OK!\"}", response.getAttachment(Http2Client.RESPONSE_BODY));
        } finally {
            // return the connection to the connection pool.
            client.returnConnection(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
    }

    @Test
    @Ignore
    // This test case needs to be fixed in the future. It is not working now.
    public void testSingleMarketAsych() throws Exception {
        callMarketApiAsync();
    }

    @Test
    public void testVerifyJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims, curr_kid, KeyUtil.deserializePrivateKey(curr_key, KeyUtil.RSA));
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(securityConfig);
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }

    @Test
    public void testVerifySign() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims, curr_kid, KeyUtil.deserializePrivateKey(curr_key, KeyUtil.RSA));
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(securityConfig);
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }

    @Test
    public void testVerifyToken() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims, curr_kid, KeyUtil.deserializePrivateKey(curr_key, KeyUtil.RSA));
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(securityConfig);
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }

}
