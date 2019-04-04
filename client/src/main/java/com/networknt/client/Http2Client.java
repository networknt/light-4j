/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.networknt.client;

import com.networknt.client.http.Light4jHttp2ClientProvider;
import com.networknt.client.http.Light4jHttpClientProvider;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.TokenManager;
import com.networknt.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.utility.ModuleRegistry;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
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

    static final String TLS = "tls";
    static final String BUFFER_SIZE = "bufferSize";
    static final String LOAD_TRUST_STORE = "loadTrustStore";
    static final String LOAD_KEY_STORE = "loadKeyStore";
    static final String TRUST_STORE = "trustStore";
    static final String KEY_STORE = "keyStore";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";

    static final String OAUTH = "oauth";
    static final String TOKEN = "token";



    static Map<String, Object> config;
    static Map<String, Object> tokenConfig;
    static Map<String, Object> secretConfig;

    // TokenManager is to manage cached jwt tokens for this client.
    private TokenManager tokenManager = TokenManager.getInstance();

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

        secretConfig = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
        if(secretConfig == null) {
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
            	addProvider(map, scheme, provider);
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

    private void addProvider(Map<String, ClientProvider> map, String scheme, ClientProvider provider) {
    	if (System.getProperty("java.version").startsWith("1.8.")) {// Java 8
        	if (Light4jHttpClientProvider.HTTPS.equalsIgnoreCase(scheme)) {
        		map.putIfAbsent(scheme, new Light4jHttpClientProvider());
        	}else if (Light4jHttp2ClientProvider.HTTP2.equalsIgnoreCase(scheme)){
        		map.putIfAbsent(scheme, new Light4jHttp2ClientProvider());
        	}else {
        		map.put(scheme, provider);
        	}
    	}else {
    		map.put(scheme, provider);
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
     * @return Result when fail to get jwt, it will return a Status.
     */
    public Result addCcToken(ClientRequest request) {
        Result<Jwt> result = tokenManager.getJwt(request);
        if(result.isFailure()) { return Failure.of(result.getError()); }
        request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + result.getResult().getJwt());
        return result;
    }

    /**
     * Add Client Credentials token cached in the client for standalone application
     *
     * This is the method called from standalone application like enterprise scheduler for batch jobs
     * or mobile apps.
     *
     * @param request the http request
     * @param traceabilityId the traceability id
     * @return Result when fail to get jwt, it will return a Status.
     */
    public Result addCcTokenTrace(ClientRequest request, String traceabilityId) {
        Result<Jwt> result = tokenManager.getJwt(request);
        if(result.isFailure()) { return Failure.of(result.getError()); }
        request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + result.getResult().getJwt());
        request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, traceabilityId);
        return result;
    }

    /**
     * Support API to API calls with scope token. The token is the original token from consumer and
     * the client credentials token of caller API is added from cache.
     *
     * This method is used in API to API call
     *
     * @param request the http request
     * @param exchange the http server exchange
     */
    public Result propagateHeaders(ClientRequest request, final HttpServerExchange exchange) {
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        return populateHeader(request, token, cid, tid);
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
     * @return Result when fail to get jwt, it will return a Status.
     */
    public Result populateHeader(ClientRequest request, String authToken, String correlationId, String traceabilityId) {
        if(traceabilityId != null) {
            addAuthTokenTrace(request, authToken, traceabilityId);
        } else {
            addAuthToken(request, authToken);
        }
        Result<Jwt> result = tokenManager.getJwt(request);
        if(result.isFailure()) { return Failure.of(result.getError()); }
        request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, correlationId);
        request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + result.getResult().getJwt());
        return result;
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

    /**
     * default method for creating ssl context. trustedNames config is not used.
     *
     * @return SSLContext
     * @throws IOException
     */
    public static SSLContext createSSLContext() throws IOException {
    	Map<String, Object> tlsMap = (Map<String, Object>)config.get(TLS);
    	
    	return null==tlsMap?null:createSSLContext((String)tlsMap.get(TLSConfig.DEFAULT_GROUP_KEY));
    }

    /**
     * create ssl context using specified trustedName config
     *
     * @param trustedNamesGroupKey - the trustedName config to be used
     * @return SSLContext
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
	public static SSLContext createSSLContext(String trustedNamesGroupKey) throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        Map<String, Object> tlsMap = (Map<String, Object>)config.get(TLS);
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
                        TLSConfig tlsConfig = TLSConfig.create(tlsMap, trustedNamesGroupKey);

                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init(trustStore);
                        trustManagers = ClientX509ExtendedTrustManager.decorate(trustManagerFactory.getTrustManagers(), tlsConfig);
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
