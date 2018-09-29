package com.networknt.client;

import com.networknt.client.oauth.ClientCredentialsRequest;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.TokenRequest;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.*;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a new client module that replaces the old Client module. The old version
 * only support HTTP 1.1 and the new version support both 1.1 and 2.0 and it is very
 * simple to use and more efficient. It is light-weight with only Undertow core dependency.
 *
 * @author Steve Hu
 */
public class Http2Client {
    static final Logger logger = LoggerFactory.getLogger(Http2Client.class);

    public static final String CONFIG_NAME = "client";
    public static final String CONFIG_SECRET = "secret";
    public static final OptionMap DEFAULT_OPTIONS = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 8)
            .set(Options.TCP_NODELAY, true)
            .set(Options.KEEP_ALIVE, true)
            .set(Options.WORKER_NAME, "Client").getMap();
    public static XnioWorker WORKER;
    public static XnioSsl SSL;
    public static int bufferSize;
    public static int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);

    static final String BUFFER_SIZE = "bufferSize";
    static final String TLS = "tls";
    static final String LOAD_TRUST_STORE = "loadTrustStore";
    static final String LOAD_KEY_STORE = "loadKeyStore";
    static final String TRUST_STORE = "trustStore";
    static final String KEY_STORE = "keyStore";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";

    static final String OAUTH = "oauth";
    static final String TOKEN = "token";
    static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";

    static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERR10009";

    static Map<String, Object> config;
    static Map<String, Object> tokenConfig;
    static Map<String, Object> secretConfig;

    // Cached jwt token for this client.
    private String jwt;
    private long expire;
    private volatile boolean renewing = false;
    private volatile long expiredRetryTimeout;
    private volatile long earlyRetryTimeout;

    private final Object lock = new Object();

    static {
        List<String> masks = new ArrayList<>();
        ModuleRegistry.registerModule(Http2Client.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), masks);
        config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        Object bufferSizeObject = config.get(BUFFER_SIZE);
        if(bufferSizeObject == null) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        } else {
            bufferSize = (int)bufferSizeObject;
        }
        if(config != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)config.get(OAUTH);
            if(oauthConfig != null) {
                tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
            }
        }

        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
        if(secretMap != null) {
            secretConfig = DecryptUtil.decryptMap(secretMap);
        } else {
            throw new ExceptionInInitializerError("Could not locate secret.yml");
        }
    }

    public static final ByteBufferPool BUFFER_POOL = new DefaultByteBufferPool(true, bufferSize * 1024);
    /**
     * @deprecated Use BUFFER_POOL instead!
     */
    @Deprecated
    public static final ByteBufferPool POOL = BUFFER_POOL;
    /**
     * @deprecated Use BUFFER_POOL instead!
     */
    @Deprecated
    public static final ByteBufferPool SSL_BUFFER_POOL = BUFFER_POOL;

    private final Map<String, ClientProvider> clientProviders;

    private static final Http2Client INSTANCE = new Http2Client();

    private Http2Client() {
        this(Http2Client.class.getClassLoader());
    }

    private Http2Client(final ClassLoader classLoader) {
        ServiceLoader<ClientProvider> providers = ServiceLoader.load(ClientProvider.class, classLoader);
        final Map<String, ClientProvider> map = new HashMap<>();
        for (ClientProvider provider : providers) {
            for (String scheme : provider.handlesSchemes()) {
                map.put(scheme, provider);
            }
        }
        this.clientProviders = Collections.unmodifiableMap(map);
        try {
            final Xnio xnio = Xnio.getInstance();
            WORKER = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
            SSL = new UndertowXnioSsl(WORKER.getXnio(), OptionMap.EMPTY, BUFFER_POOL, createSSLContext());
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
    }

    public IoFuture<ClientConnection> connect(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        return connect(uri, worker, null, bufferPool, options);
    }

    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        return connect(bindAddress, uri, worker, null, bufferPool, options);
    }

    public IoFuture<ClientConnection> connect(final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        return connect((InetSocketAddress) null, uri, worker, ssl, bufferPool, options);
    }

    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        final FutureResult<ClientConnection> result = new FutureResult<>();
        provider.connect(new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection r) {
                result.setResult(r);
            }

            @Override
            public void failed(IOException e) {
                result.setException(e);
            }
        }, bindAddress, uri, worker, ssl, bufferPool, options);
        return result.getIoFuture();
    }

    public IoFuture<ClientConnection> connect(final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        return connect((InetSocketAddress) null, uri, ioThread, null, bufferPool, options);
    }


    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        return connect(bindAddress, uri, ioThread, null, bufferPool, options);
    }

    public IoFuture<ClientConnection> connect(final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        return connect((InetSocketAddress) null, uri, ioThread, ssl, bufferPool, options);
    }

    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        final FutureResult<ClientConnection> result = new FutureResult<>();
        provider.connect(new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection r) {
                result.setResult(r);
            }

            @Override
            public void failed(IOException e) {
                result.setException(e);
            }
        }, bindAddress, uri, ioThread, ssl, bufferPool, options);
        return result.getIoFuture();
    }

    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, uri, worker, null, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, bindAddress, uri, worker, null, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, uri, worker, ssl, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, bindAddress, uri, worker, ssl, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, uri, ioThread, null, bufferPool, options);
    }


    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, bindAddress, uri, ioThread, null, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, uri, ioThread, ssl, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, bindAddress, uri, ioThread, ssl, bufferPool, options);
    }

    private ClientProvider getClientProvider(URI uri) {
        return clientProviders.get(uri.getScheme());
    }

    public static Http2Client getInstance() {
        return INSTANCE;
    }

    public static Http2Client getInstance(final ClassLoader classLoader) {
        return new Http2Client(classLoader);
    }


    /**
     * Add Authorization Code grant token the caller app gets from OAuth2 server.
     *
     * This is the method called from client like web server
     *
     * @param request the http request
     * @param token the bearer token
     */
    public void addAuthToken(ClientRequest request, String token) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                // other cases of Bearer
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
    }

    /**
     * Add Authorization Code grant token the caller app gets from OAuth2 server and add traceabilityId
     *
     * This is the method called from client like web server that want to have traceabilityId pass through.
     *
     * @param request the http request
     * @param token the bearer token
     * @param traceabilityId the traceability id
     */
    public void addAuthTokenTrace(ClientRequest request, String token, String traceabilityId) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                // other cases of Bearer
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
        request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, traceabilityId);
    }

    /**
     * Add Client Credentials token cached in the client for standalone application
     *
     * This is the method called from standalone application like enterprise scheduler for batch jobs
     * or mobile apps.
     *
     * @param request the http request
     * @throws ClientException client exception
     * @throws ApiException api exception
     */
    public void addCcToken(ClientRequest request) throws ClientException, ApiException {
        checkCCTokenExpired();
        request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + jwt);
    }

    /**
     * Add Client Credentials token cached in the client for standalone application
     *
     * This is the method called from standalone application like enterprise scheduler for batch jobs
     * or mobile apps.
     *
     * @param request the http request
     * @param traceabilityId the traceability id
     * @throws ClientException client exception
     * @throws ApiException api exception
     */
    public void addCcTokenTrace(ClientRequest request, String traceabilityId) throws ClientException, ApiException {
        checkCCTokenExpired();
        request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + jwt);
        request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, traceabilityId);
    }

    /**
     * Support API to API calls with scope token. The token is the original token from consumer and
     * the client credentials token of caller API is added from cache.
     *
     * This method is used in API to API call
     *
     * @param request the http request
     * @param exchange the http server exchange
     * @throws ClientException client exception
     * @throws ApiException api exception
     */
    public void propagateHeaders(ClientRequest request, final HttpServerExchange exchange) throws ClientException, ApiException {
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        populateHeader(request, token, cid, tid);
    }

    /**
     * Support API to API calls with scope token. The token is the original token from consumer and
     * the client credentials token of caller API is added from cache. authToken, correlationId and
     * traceabilityId are passed in as strings.
     *
     * This method is used in API to API call
     *
     * @param request the http request
     * @param authToken the authorization token
     * @param correlationId the correlation id
     * @param traceabilityId the traceability id
     * @throws ClientException client exception
     * @throws ApiException api exception
     */
    public void populateHeader(ClientRequest request, String authToken, String correlationId, String traceabilityId) throws ClientException, ApiException {
        if(traceabilityId != null) {
            addAuthTokenTrace(request, authToken, traceabilityId);
        } else {
            addAuthToken(request, authToken);
        }
        request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, correlationId);
        checkCCTokenExpired();
        request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + jwt);
    }

    private void getCCToken() throws ClientException {
        TokenRequest tokenRequest = new ClientCredentialsRequest();
        TokenResponse tokenResponse = OauthHelper.getToken(tokenRequest);
        synchronized (lock) {
            jwt = tokenResponse.getAccessToken();
            // the expiresIn is seconds and it is converted to millisecond in the future.
            expire = System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000;
            logger.info("Get client credentials token {} with expire_in {} seconds", jwt, tokenResponse.getExpiresIn());
        }
    }

    private void checkCCTokenExpired() throws ClientException, ApiException {
        long tokenRenewBeforeExpired = (Integer) tokenConfig.get(TOKEN_RENEW_BEFORE_EXPIRED);
        long expiredRefreshRetryDelay = (Integer)tokenConfig.get(EXPIRED_REFRESH_RETRY_DELAY);
        long earlyRefreshRetryDelay = (Integer)tokenConfig.get(EARLY_REFRESH_RETRY_DELAY);
        boolean isInRenewWindow = expire - System.currentTimeMillis() < tokenRenewBeforeExpired;
        if(logger.isTraceEnabled()) logger.trace("isInRenewWindow = " + isInRenewWindow);
        if(isInRenewWindow) {
            if(expire <= System.currentTimeMillis()) {
                if(logger.isTraceEnabled()) logger.trace("In renew window and token is expired.");
                // block other request here to prevent using expired token.
                synchronized (Http2Client.class) {
                    if(expire <= System.currentTimeMillis()) {
                        if(logger.isTraceEnabled()) logger.trace("Within the synch block, check if the current request need to renew token");
                        if(!renewing || System.currentTimeMillis() > expiredRetryTimeout) {
                            // if there is no other request is renewing or the renewing flag is true but renewTimeout is passed
                            renewing = true;
                            expiredRetryTimeout = System.currentTimeMillis() + expiredRefreshRetryDelay;
                            if(logger.isTraceEnabled()) logger.trace("Current request is renewing token synchronously as token is expired already");
                            getCCToken();
                            renewing = false;
                        } else {
                            if(logger.isTraceEnabled()) logger.trace("Circuit breaker is tripped and not timeout yet!");
                            // reject all waiting requests by thrown an exception.
                            throw new ApiException(new Status(STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE));
                        }
                    }
                }
            } else {
                // Not expired yet, try to renew async but let requests use the old token.
                if(logger.isTraceEnabled()) logger.trace("In renew window but token is not expired yet.");
                synchronized (Http2Client.class) {
                    if(expire > System.currentTimeMillis()) {
                        if(!renewing || System.currentTimeMillis() > earlyRetryTimeout) {
                            renewing = true;
                            earlyRetryTimeout = System.currentTimeMillis() + earlyRefreshRetryDelay;
                            if(logger.isTraceEnabled()) logger.trace("Retrieve token async is called while token is not expired yet");

                            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                            executor.schedule(() -> {
                                try {
                                    getCCToken();
                                    renewing = false;
                                    if(logger.isTraceEnabled()) logger.trace("Async get token is completed.");
                                } catch (Exception e) {
                                    logger.error("Async retrieve token error", e);
                                    // swallow the exception here as it is on a best effort basis.
                                }
                            }, 50, TimeUnit.MILLISECONDS);
                            executor.shutdown();
                        }
                    }
                }
            }
        }
        if(logger.isTraceEnabled()) logger.trace("Check secondary token is done!");
    }

    private static KeyStore loadKeyStore(final String name, final char[] password) throws IOException {
        final InputStream stream = Config.getInstance().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, password);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    public static SSLContext createSSLContext() throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        Map<String, Object> tlsMap = (Map)config.get(TLS);
        if(tlsMap != null) {
            try {
                // load key store for client certificate if two way ssl is used.
                Boolean loadKeyStore = (Boolean) tlsMap.get(LOAD_KEY_STORE);
                if (loadKeyStore != null && loadKeyStore) {
                    String keyStoreName = (String)tlsMap.get(KEY_STORE);
                    String keyStorePass = (String)secretConfig.get(SecretConstants.CLIENT_KEYSTORE_PASS);
                    String keyPass = (String)secretConfig.get(SecretConstants.CLIENT_KEY_PASS);
                    KeyStore keyStore = loadKeyStore(keyStoreName, keyStorePass.toCharArray());
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(keyStore, keyPass.toCharArray());
                    keyManagers = keyManagerFactory.getKeyManagers();
                }
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
                throw new IOException("Unable to initialise KeyManager[]", e);
            }

            TrustManager[] trustManagers = null;
            try {
                // load trust store, this is the server public key certificate
                // first check if javax.net.ssl.trustStore system properties is set. It is only necessary if the server
                // certificate doesn't have the entire chain.
                Boolean loadTrustStore = (Boolean) tlsMap.get(LOAD_TRUST_STORE);
                if (loadTrustStore != null && loadTrustStore) {
                    String trustStoreName = System.getProperty(TRUST_STORE_PROPERTY);
                    String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
                    if (trustStoreName != null && trustStorePass != null) {
                        if(logger.isInfoEnabled()) logger.info("Loading trust store from system property at " + Encode.forJava(trustStoreName));
                    } else {
                        trustStoreName = (String) tlsMap.get(TRUST_STORE);
                        trustStorePass = (String)secretConfig.get(SecretConstants.CLIENT_TRUSTSTORE_PASS);
                        if(logger.isInfoEnabled()) logger.info("Loading trust store from config at " + Encode.forJava(trustStoreName));
                    }
                    if (trustStoreName != null && trustStorePass != null) {
                        KeyStore trustStore = loadKeyStore(trustStoreName, trustStorePass.toCharArray());
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init(trustStore);
                        trustManagers = trustManagerFactory.getTrustManagers();
                    }
                }
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new IOException("Unable to initialise TrustManager[]", e);
            }

            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, trustManagers, null);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Unable to create and initialise the SSLContext", e);
            }
        } else {
            logger.error("TLS configuration section is missing in client.yml");
        }

        return sslContext;
    }

    public static String getFormDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8").replaceAll("\\+", "%20"));
        }
        return result.toString();
    }

    public ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch) {
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
                                result.getResponse().putAttachment(RESPONSE_BODY, string);
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                logger.error("IOException:", e);
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        logger.error("IOException:", e);
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
                    logger.error("IOException:", e);
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                logger.error("IOException:", e);
                latch.countDown();
            }
        };
    }

    public ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch, final String requestBody) {
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        reference.set(result.getResponse());
                        new StringReadChannelListener(BUFFER_POOL) {
                            @Override
                            protected void stringDone(String string) {
                                result.getResponse().putAttachment(RESPONSE_BODY, string);
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                logger.error("IOException:", e);
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        logger.error("IOException:", e);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void failed(IOException e) {
                logger.error("IOException:", e);
                latch.countDown();
            }
        };
    }

    public ClientCallback<ClientExchange> createFullCallback(final AtomicReference<AsyncResult<AsyncResponse>> reference, final CountDownLatch latch) {
        final long startTime = System.currentTimeMillis();
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        new StringReadChannelListener(result.getConnection().getBufferPool()) {

                            @Override
                            protected void stringDone(String string) {
                                AsyncResponse ar = new AsyncResponse(result.getResponse(), string, System.currentTimeMillis() - startTime);
                                reference.set(DefaultAsyncResult.succeed(ar));
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                reference.set(DefaultAsyncResult.fail(e));
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        reference.set(DefaultAsyncResult.fail(e));
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
                    reference.set(DefaultAsyncResult.fail(e));
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                reference.set(DefaultAsyncResult.fail(e));
                latch.countDown();
            }
        };
    }

    public ClientCallback<ClientExchange> createFullCallback(final AtomicReference<AsyncResult<AsyncResponse>> reference, final CountDownLatch latch, final String requestBody) {
        final long startTime = System.currentTimeMillis();
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        new StringReadChannelListener(BUFFER_POOL) {
                            @Override
                            protected void stringDone(String string) {
                                AsyncResponse ar = new AsyncResponse(result.getResponse(), string, System.currentTimeMillis() - startTime);
                                reference.set(DefaultAsyncResult.succeed(ar));
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                reference.set(DefaultAsyncResult.fail(e));
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        reference.set(DefaultAsyncResult.fail(e));
                        latch.countDown();
                    }
                });
            }

            @Override
            public void failed(IOException e) {
                reference.set(DefaultAsyncResult.fail(e));
                latch.countDown();
            }
        };
    }

}
