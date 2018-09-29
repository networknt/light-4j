package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.io.Receiver;
import io.undertow.io.Sender;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.lang.JoseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class Http2ClientTest {
    static final Logger logger = LoggerFactory.getLogger(Http2ClientTest.class);
    static Undertow server = null;
    static SSLContext sslContext;
    private static final String message = "Hello World!";
    public static final String MESSAGE = "/message";
    public static final String POST = "/post";
    public static final String FORM = "/form";
    public static final String TOKEN = "/oauth2/token";
    public static final String API = "/api";
    public static final String KEY = "/oauth2/key";

    private static final String SERVER_KEY_STORE = "server.keystore";
    private static final String SERVER_TRUST_STORE = "server.truststore";
    private static final String CLIENT_KEY_STORE = "client.keystore";
    private static final String CLIENT_TRUST_STORE = "client.truststore";
    private static final char[] STORE_PASSWORD = "password".toCharArray();

    private static XnioWorker worker;

    private static final URI ADDRESS;



    static {
        try {
            ADDRESS = new URI("http://localhost:7777");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendMessage(final HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, message.length() + "");
        final Sender sender = exchange.getResponseSender();
        sender.send(message);
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        // Create xnio worker
        final Xnio xnio = Xnio.getInstance();
        final XnioWorker xnioWorker = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
        worker = xnioWorker;

        if(server == null) {
            System.out.println("starting server");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            builder.addHttpsListener(7778, "localhost", sslContext);
            builder.addHttpListener(7777, "localhost");

            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);


            server = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(MESSAGE, exchange -> sendMessage(exchange))
                            .addExactPath(KEY, exchange -> sendMessage(exchange))
                            .addExactPath(API, (exchange) -> {
                                boolean hasScopeToken = exchange.getRequestHeaders().contains(HttpStringConstants.SCOPE_TOKEN);
                                Assert.assertTrue(hasScopeToken);
                                String scopeToken = exchange.getRequestHeaders().get(HttpStringConstants.SCOPE_TOKEN, 0);
                                boolean expired = isTokenExpired(scopeToken);
                                Assert.assertFalse(expired);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(ByteBuffer.wrap(
                                        Config.getInstance().getMapper().writeValueAsBytes(
                                                Collections.singletonMap("message", "OK!"))));

                            })
                            .addExactPath(FORM, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    exchange.getResponseSender().send(message);
                                }
                            }))
                            .addExactPath(TOKEN, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    try {
                                        int sleepTime = randInt(1, 3) * 1000;
                                        if(sleepTime >= 2000) {
                                            sleepTime = 3000;
                                        } else {
                                            sleepTime = 1000;
                                        }
                                        Thread.sleep(sleepTime);
                                        // create a token that expired in 5 seconds.
                                        Map<String, Object> map = new HashMap<>();
                                        String token = getJwt(5);
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
                            }))
                            .addExactPath(POST, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    exchange.getResponseSender().send(message);
                                }
                            })))
                    .setWorkerThreads(200)
                    .build();

            server.start();
        }
    }

    @AfterClass
    public static void afterClass() {
        worker.shutdown();
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server.stop();
            System.out.println("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

    }

    static Http2Client createClient() {
        return createClient(OptionMap.EMPTY);
    }

    static Http2Client createClient(final OptionMap options) {
        return Http2Client.getInstance();
    }

    @Test
    public void testAddNullToken() {
        final Http2Client client = Http2Client.getInstance();
        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
        client.addAuthToken(request, null);
        Assert.assertNull(request.getRequestHeaders().getFirst(Headers.AUTHORIZATION));
    }

    @Test
    public void testAddToken() {
        final Http2Client client = Http2Client.getInstance();
        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
        client.addAuthToken(request, "token");
        Assert.assertEquals("Bearer token", request.getRequestHeaders().getFirst(Headers.AUTHORIZATION));
    }

    @Test
    public void testSingleHttp2PostSsl() throws Exception {
        //
        final Http2Client client = createClient();
        final String postMessage = "This is a post request";

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(POST);
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                            result.setResponseListener(new ClientCallback<ClientExchange>() {
                                @Override
                                public void completed(ClientExchange result) {
                                    new StringReadChannelListener(Http2Client.BUFFER_POOL) {

                                        @Override
                                        protected void stringDone(String string) {
                                            responses.add(string);
                                            latch.countDown();
                                        }

                                        @Override
                                        protected void error(IOException e) {
                                            e.printStackTrace();
                                            latch.countDown();
                                        }
                                    }.setup(result.getResponseChannel());
                                }

                                @Override
                                public void failed(IOException e) {
                                    e.printStackTrace();
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void failed(IOException e) {
                            e.printStackTrace();
                            latch.countDown();
                        }
                    });
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(1, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testSingleHttp2FormSsl() throws Exception {
        //
        final Http2Client client = createClient();
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        final String postMessage = client.getFormDataString(params);

        final List<String> responses = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        SSLContext context = client.createSSLContext();
        XnioSsl ssl = new UndertowXnioSsl(worker.getXnio(), OptionMap.EMPTY, Http2Client.BUFFER_POOL, context);

        final ClientConnection connection = client.connect(new URI("https://localhost:7778"), worker, ssl, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(FORM);
                    request.getRequestHeaders().put(Headers.HOST, "localhost");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            new StringWriteChannelListener(postMessage).setup(result.getRequestChannel());
                            result.setResponseListener(new ClientCallback<ClientExchange>() {
                                @Override
                                public void completed(ClientExchange result) {
                                    new StringReadChannelListener(Http2Client.BUFFER_POOL) {

                                        @Override
                                        protected void stringDone(String string) {
                                            System.out.println("string = " + string);
                                            responses.add(string);
                                            latch.countDown();
                                        }

                                        @Override
                                        protected void error(IOException e) {
                                            e.printStackTrace();
                                            latch.countDown();
                                        }
                                    }.setup(result.getResponseChannel());
                                }

                                @Override
                                public void failed(IOException e) {
                                    e.printStackTrace();
                                    latch.countDown();
                                }
                            });
                        }

                        @Override
                        public void failed(IOException e) {
                            e.printStackTrace();
                            latch.countDown();
                        }
                    });
                }
            });

            latch.await(10, TimeUnit.SECONDS);

            Assert.assertEquals(1, responses.size());
            for (final String response : responses) {
                Assert.assertEquals(postMessage, response);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testConnectionClose() throws Exception {
        //
        final Http2Client client = createClient();

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        try {
            ClientRequest request = new ClientRequest().setPath(MESSAGE).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            final ClientResponse response = reference.get();
            Assert.assertEquals(message, response.getAttachment(Http2Client.RESPONSE_BODY));
            Assert.assertEquals(false, connection.isOpen());
        } finally {
            IoUtils.safeClose(connection);
        }

    }

    @Test
    public void testResponseTime() throws Exception {
        //
        final Http2Client client = createClient();

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        try {
            ClientRequest request = new ClientRequest().setPath(MESSAGE).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            final AtomicReference<AsyncResult<AsyncResponse>> reference = new AtomicReference<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createFullCallback(reference, latch));
            latch.await();
            final AsyncResult<AsyncResponse> ar = reference.get();
            if(ar.succeeded()) {
                Assert.assertEquals(message, ar.result().getResponseBody());
                Assert.assertTrue(ar.result().getResponseTime() > 0);
                System.out.println("responseTime = " + ar.result().getResponseTime());
            } else {
                ar.cause().printStackTrace();
            }
            Assert.assertEquals(false, connection.isOpen());
        } finally {
            IoUtils.safeClose(connection);
        }
    }


    @Test
    public void testSingleAsych() throws Exception {
        callApiAsync();
    }

    @Test
    public void testGetFormDataString() throws UnsupportedEncodingException {
        // This is to reproduce and fix #172
        Map<String, String> params = new HashMap<>();
        params.put("scope", "a b c d");
        String s = Http2Client.getFormDataString(params);
        Assert.assertEquals("scope=a%20b%20c%20d", s);
    }

    public String callApiAsync() throws Exception {
        final Http2Client client = createClient();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection = client.connect(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(API).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            client.populateHeader(request, "Bearer token", "cid", "tid");
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            final ClientResponse response = reference.get();
            Assert.assertEquals("{\"message\":\"OK!\"}", response.getAttachment(Http2Client.RESPONSE_BODY));
            Assert.assertEquals(false, connection.isOpen());
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
    }

    @Test
    public void testAsyncAboutToExpire() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 1; i++) {
            callApiAsyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testAsyncExpired() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 1; i++) {
            callApiAsyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testMixed() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 1; i++) {
            callApiAsyncMultiThread(4
            );
            logger.info("called times: " + i);
            try {
                int sleepTime = randInt(1, 6) * 1000;
                if (sleepTime > 3000) {
                    sleepTime = 6000;
                } else {
                    sleepTime = 1000;
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void callApiAsyncMultiThread(final int threadCount) throws InterruptedException, ExecutionException {
        Callable<String> task = this::callApiAsync;
        List<Callable<String>> tasks = Collections.nCopies(threadCount, task);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        for (Future<String> future : futures) {
            resultList.add(future.get());
        }
        System.out.println("resultList = " + resultList);
    }

    /*
    private ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch) {
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        reference.set(result.getResponse());
                        new StringReadChannelListener(result.getConnection().getBufferPool()) {

                            @Override
                            protected void stringDone(String string) {
                                result.getResponse().putAttachment(Http2Client.RESPONSE_BODY, string);
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                e.printStackTrace();

                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();

                        latch.countDown();
                    }
                });
                try {
                    result.getRequestChannel().shutdownWrites();
                    if(!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        };
    }
    */

    private static KeyStore loadKeyStore(final String name) throws IOException {
        final InputStream stream = Config.getInstance().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, STORE_PASSWORD);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, boolean client) throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        SSLContext sslContext;
        try {
            if(!client) {
                sslContext = SSLContext.getInstance("TLS");
            } else {
                sslContext = SSLContext.getInstance("TLS");
            }
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

    private static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max-min) + 1) + min;
    }

    private static boolean isTokenExpired(String authorization) {
        boolean expired = false;
        String jwt = getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtConsumer consumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setDisableRequireSignature()
                        .setSkipSignatureVerification()
                        .build();

                JwtContext jwtContext = consumer.process(jwt);
                JwtClaims jwtClaims = jwtContext.getJwtClaims();

                try {
                    if ((NumericDate.now().getValue() - 60) >= jwtClaims.getExpirationTime().getValue()) {
                        expired = true;
                    }
                } catch (MalformedClaimException e) {
                    logger.error("MalformedClaimException:", e);
                }
            } catch(InvalidJwtException e) {
                e.printStackTrace();
            }
        }
        return expired;
    }

    private static String getJwt(int expiredInSeconds) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return getJwt(claims);
    }

    private static JwtClaims getTestClaims() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("urn:com:networknt:oauth2:v1");
        claims.setAudience("urn:com.networknt");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("version", "1.0");

        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    public static String getJwtFromAuthorization(String authorization) {
        String jwt = null;
        if(authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    jwt = credentials;
                }
            }
        }
        return jwt;
    }

    public static String getJwt(JwtClaims claims) throws JoseException {
        String jwt;

        RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(
                "/config/primary.jks", "password", "selfsigned");

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS nested inside a JWE
        // So we first create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the sender's private key
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue("100");

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        jwt = jws.getCompactSerialization();
        return jwt;
    }

    private static PrivateKey getPrivateKey(String filename, String password, String key) {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Http2Client.class.getResourceAsStream(filename),
                    password.toCharArray());

            privateKey = (PrivateKey) keystore.getKey(key,
                    password.toCharArray());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        if (privateKey == null) {
            logger.error("Failed to retrieve private key from keystore");
        }

        return privateKey;
    }

}
