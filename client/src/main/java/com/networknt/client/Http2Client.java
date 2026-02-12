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

import com.networknt.client.circuitbreaker.CircuitBreaker;
import com.networknt.client.http.*;
import com.networknt.client.listener.ByteBufferReadChannelListener;
import com.networknt.client.listener.ByteBufferWriteChannelListener;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.TokenManager;
import com.networknt.client.simplepool.ConnectionHealthChecker;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.client.simplepool.SimpleConnectionMaker;
import com.networknt.client.simplepool.SimplePoolMetrics;
import com.networknt.client.simplepool.SimpleURIConnectionPool;
import com.networknt.client.simplepool.undertow.SimpleClientConnectionMaker;
import com.networknt.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.client.ssl.CompositeX509TrustManager;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.config.TlsUtil;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.server.ServerConfig;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.utility.StringUtils;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.undertow.client.http.HttpClientProvider.DISABLE_HTTPS_ENDPOINT_IDENTIFICATION_PROPERTY;

/**
 * This is a new client module that replaces the old Client module. The old version
 * only support HTTP 1.1 and the new version support both 1.1 and 2.0 and it is very
 * simple to use and more efficient. It is light-weight with only Undertow core dependency.
 *
 * @author Steve Hu
 */
public class Http2Client {
    private static final Logger logger = LoggerFactory.getLogger(Http2Client.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";
    /**
     * The name of the configuration for this client.
     */
    public static final String CONFIG_NAME = "client";

    /**
     * The default options for the Xnio worker.
     */
    public static final OptionMap DEFAULT_OPTIONS = OptionMap.builder()
            .set(Options.WORKER_IO_THREADS, 8)
            .set(Options.TCP_NODELAY, true)
            .set(Options.KEEP_ALIVE, true)
            .set(Options.WORKER_NAME, "Client").getMap();
    private static volatile ClientConfig clientConfig = ClientConfig.get();

    /**
     * The Xnio worker used by this client.
     */
    public static XnioWorker WORKER;

    /**
     * @deprecated  As of release 1.6.11, replaced by {@link #getDefaultXnioSsl()}
     *              SSL is no longer statically initialized.
     */
    @Deprecated
    public static volatile XnioSsl SSL;

    /**
     * The attachment key for the response body.
     */
    public static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);

    /**
     * The attachment key for the response body as a ByteBuffer.
     */
    public static AttachmentKey<ByteBuffer> BUFFER_BODY = AttachmentKey.create(ByteBuffer.class);

    /**
     * The scheme for secure HTTP connections.
     */
    public static final String HTTPS = "https";

    /**
     * The scheme for TLS connections.
     */
    public static final String TLS = "tls";
    static final String LOAD_TRUST_STORE = "loadTrustStore";
    static final String LOAD_KEY_STORE = "loadKeyStore";
    static final String LOAD_DEFAULT_TRUST = "loadDefaultTrustStore";
    static final String TRUST_STORE = "trustStore";
    static final String TRUST_STORE_PASS = "trustStorePass";
    static final String DEFAULT_CERT_PASS = "defaultCertPassword";
    static final String KEY_STORE = "keyStore";
    static final String KEY_STORE_PASS = "keyStorePass";
    static final String KEY_PASS = "keyPass";
    static final String TLS_VERSION = "tlsVersion";
    static final String KEY_STORE_PROPERTY = "javax.net.ssl.keyStore";
    static final String KEY_STORE_PASSWORD_PROPERTY = "javax.net.ssl.keyStorePassword";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    static final String TRUST_STORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";
    static final String SERVICE_ID = "serviceId";
    static String callerId = "unknown";
    static String MASK_KEY_CLIENT_SECRET = "client_secret";
    static String MASK_KEY_TRUST_STORE_PASS = "trustStorePass";
    static String MASK_KEY_KEY_STORE_PASS = "keyStorePass";
    static String MASK_KEY_KEY_PASS = "keyPass";


    // TokenManager is to manage cached jwt tokens for this client.
    private final TokenManager tokenManager = TokenManager.getInstance();

    // This is the old connection pool that is kept for backward compatibility.
    private final Http2ClientConnectionPool http2ClientConnectionPool = Http2ClientConnectionPool.getInstance();
    // This is the new connection pool that is used by the new request method.
    private final Map<URI, SimpleURIConnectionPool> pools = new ConcurrentHashMap<>();

    // Pool metrics (optional, enabled via configuration)
    private final SimplePoolMetrics poolMetrics;
    // Connection health checker (optional, enabled via configuration)
    private ConnectionHealthChecker healthChecker;

    // Tracks ClientConnection → ConnectionToken for deprecated method redirection
    // This allows returnConnection() to find the token for restore()
    private final Map<ClientConnection, SimpleConnectionHolder.ConnectionToken> connectionTokenMap = new ConcurrentHashMap<>();

    /**
     * The buffer pool used by this client.
     */
    public static ByteBufferPool BUFFER_POOL = new DefaultByteBufferPool(true, ClientConfig.get().getBufferSize() * 1024);

    /**
     * The client providers used by this client, keyed by scheme.
     */
    protected final Map<String, ClientProvider> clientProviders;

    private static volatile Http2Client INSTANCE;

    /**
     * Constructs an Http2Client using the default class loader.
     */
    protected Http2Client() {
        this(Http2Client.class.getClassLoader());
    }

    private Http2Client(final ClassLoader classLoader) {
        Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
        // disable the hostname verification based on the config.
        if(tlsMap == null || tlsMap.get(TLSConfig.VERIFY_HOSTNAME) == null || Boolean.FALSE.equals(Config.loadBooleanValue(TLSConfig.VERIFY_HOSTNAME, tlsMap.get(TLSConfig.VERIFY_HOSTNAME)))) {
            System.setProperty(DISABLE_HTTPS_ENDPOINT_IDENTIFICATION_PROPERTY, "true");
        }
        boolean injectCallerId = ClientConfig.get().getRequest().isInjectCallerId();
        if(injectCallerId) {
            ServerConfig serverConfig = ServerConfig.load();
            if(serverConfig != null) {
                callerId = serverConfig.getServiceId();
            }
        }

        ServiceLoader<ClientProvider> providers = ServiceLoader.load(ClientProvider.class, classLoader);
        final Map<String, ClientProvider> map = new HashMap<>();
        for (ClientProvider provider : providers) {
            for (String scheme : provider.handlesSchemes()) {
            	addProvider(map, scheme, provider);
            }
        }
        this.clientProviders = Collections.unmodifiableMap(map);
        try {
            final Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
            WORKER = xnio.createWorker(null, Http2Client.DEFAULT_OPTIONS);
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }

        // Initialize pool metrics if enabled
        RequestConfig requestConfig = ClientConfig.get().getRequest();
        if (requestConfig != null && Boolean.TRUE.equals(requestConfig.isPoolMetricsEnabled())) {
            this.poolMetrics = new SimplePoolMetrics();
            logger.info("SimplePool metrics enabled");
        } else {
            this.poolMetrics = null;
        }

        // Initialize health checker if enabled
        if (requestConfig != null && Boolean.TRUE.equals(requestConfig.isHealthCheckEnabled())) {
            int intervalMs = requestConfig.getHealthCheckIntervalMs();
            this.healthChecker = new ConnectionHealthChecker(() -> pools, intervalMs);
            this.healthChecker.start();
            logger.info("Connection health checker started with interval {}ms", intervalMs);
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

    /**
     * Create an XnioSsl object with the given sslContext. This is used to create the normal client context
     * and the light-config-server bootstrap context separately. the XnioSsl object can be used to create
     * an Https connection to the downstream services.
     *
     * @param sslContext SslContext
     * @return XnioSsl
     */
    public XnioSsl createXnioSsl(SSLContext sslContext) {
        return new UndertowXnioSsl(WORKER.getXnio(), OptionMap.EMPTY, BUFFER_POOL, sslContext);
    }

    /**
     * This is a public static method to get a ClientConnection object returned
     * from the borrowConnection methods.
     *
     * @param timeoutSeconds timeout in seconds
     * @param future IoFuture object that contains the ClientConnection.
     * @return ClientConnection object.
     */
    @Deprecated
    public ClientConnection getFutureConnection(long timeoutSeconds, IoFuture<ClientConnection> future) {
        if(future.await(timeoutSeconds, TimeUnit.SECONDS) != IoFuture.Status.DONE) {
            throw new RuntimeException("Connection establishment timed out");
        }

        ClientConnection connection = null;
        try {
            connection = future.get();
        } catch (IOException e) {
            throw new RuntimeException("Connection establishment generated I/O exception", e);
        }

        if(connection == null)
            throw new RuntimeException("Connection establishment failed (null) - Full connection terminated");

        return connection;
    }
    /**
     * Connects to the given URI using the given worker, buffer pool, and options.
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        return connect(uri, worker, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI and worker, specifying if it is HTTP/2.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param isHttp2 true if HTTP/2
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, boolean isHttp2) {
        if(logger.isDebugEnabled()) logger.debug("The connection is http2?:" + isHttp2);
        return borrowConnection(uri, worker,  bufferPool, isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
    }

    /**
     * Borrows a connection token from the pool for the given URI and worker.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param isHttp2 true if HTTP/2 is enabled, false otherwise
     * @return a ConnectionToken
     */
    public SimpleConnectionHolder.ConnectionToken borrow(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, boolean isHttp2) {
        if(logger.isDebugEnabled()) logger.debug("The connection is http2?:" + isHttp2);
        return borrow(uri, worker,  bufferPool, isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
    }

    /**
     * Borrows a connection for the given URI, worker, and options.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        return connect(uri, worker, null, bufferPool, options);
    }

    /**
     * Borrows a connection token from the pool for the given URI, worker, and options.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return a ConnectionToken
     */
    public SimpleConnectionHolder.ConnectionToken borrow(final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        SimpleURIConnectionPool pool = pools.get(uri);
        if(pool == null) {
            SimpleConnectionMaker undertowConnectionMaker = SimpleClientConnectionMaker.instance();
            ClientConfig config = ClientConfig.get();
            pool = new SimpleURIConnectionPool(uri, config.getConnectionExpireTime(), config.getConnectionPoolSize(), null, worker, bufferPool, null, options, undertowConnectionMaker);
            pools.putIfAbsent(uri, pool);
        }
        return pool.borrow(ClientConfig.get().getRequest().getConnectTimeout());
    }

    /**
     * Borrows a connection for the given URI, worker, and options, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     * @deprecated Use {@link #borrow(URI, XnioWorker, ByteBufferPool, OptionMap)} instead.
     *             This method now redirects to SimplePool internally.
     * @see <a href="https://doc.networknt.com/concern/module/simplepool-migration">Migration Guide</a>
     */
    @Deprecated(forRemoval = true)
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        // Redirect to SimplePool
        SimpleConnectionHolder.ConnectionToken token = borrow(uri, worker, bufferPool, options);
        if (token != null && token.connection() != null) {
            ClientConnection connection = (ClientConnection) token.getRawConnection();
            connectionTokenMap.put(connection, token);  // Track for returnConnection
            return connection;
        }
        return null;
    }

    /**
     * Connects to the given URI with a bind address, using the given worker, buffer pool, and options.
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        return connect(bindAddress, uri, worker, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI and worker with a bind address.
     * @param bindAddress the local address to bind to
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        return connect(bindAddress, uri, worker, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI and worker with a bind address and a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param bindAddress the local address to bind to
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        IoFuture<ClientConnection> future = borrowConnection(bindAddress, uri, worker, bufferPool, options);
        return getFutureConnection(timeoutSeconds, future);
    }

    /**
     * Returns the default XnioSsl instance.
     * @return the default XnioSsl instance
     */
    public XnioSsl getDefaultXnioSsl() {
        if(SSL == null || clientConfig != ClientConfig.get()) {
            synchronized (Http2Client.class) {
                if(SSL == null || clientConfig != ClientConfig.get()) {
                    clientConfig = ClientConfig.get();
                    try {
                        SSL = createXnioSsl(createSSLContext());
                    } catch (Exception e) {
                        logger.error("Exception", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return SSL;
    }
    /**
     * Returns a connection to the pool.
     * @param connection the connection to return
     * @deprecated Use {@link #restore(SimpleConnectionHolder.ConnectionToken)} instead.
     *             This method now redirects to SimplePool when possible.
     *             Will be removed in a future major version.
     * @see <a href="https://doc.networknt.com/concern/module/simplepool-migration">Migration Guide</a>
     */
    @Deprecated(forRemoval = true)
    public void returnConnection(ClientConnection connection) {
        if (connection == null) return;

        // Check if we have a token for this connection (from redirected borrow)
        SimpleConnectionHolder.ConnectionToken token = connectionTokenMap.remove(connection);
        if (token != null) {
            // Redirect to SimplePool
            restore(token);
            return;
        }

        // Fallback to old pool for connections not borrowed via redirect
        http2ClientConnectionPool.resetConnectionStatus(connection);
    }

    /**
     * Restores a connection token to the pool.
     * @param token the connection token to restore
     */
    public void restore(SimpleConnectionHolder.ConnectionToken token) {
        if(token == null) return;
        SimpleURIConnectionPool pool = pools.get(token.uri());
        if(pool != null) pool.restore(token);
    }

    /**
     * Returns the pool metrics if metrics are enabled.
     * @return SimplePoolMetrics or null if metrics are disabled
     */
    public SimplePoolMetrics getPoolMetrics() {
        return poolMetrics;
    }

    /**
     * Warm up the connection pool for a specific URI.
     * Pre-establishes connections to reduce latency on first request.
     *
     * @param uri the URI to warm up connections for
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param ssl the Xnio SSL context (can be null for http)
     * @param options the option map
     * @return the number of connections actually created
     */
    public int warmUpPool(URI uri, XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        RequestConfig requestConfig = ClientConfig.get().getRequest();
        if (requestConfig == null || !Boolean.TRUE.equals(requestConfig.isPoolWarmUpEnabled())) {
            logger.debug("Pool warm-up is disabled");
            return 0;
        }

        if (HTTPS.equals(uri.getScheme()) && ssl == null) {
            ssl = getDefaultXnioSsl();
        }

        SimpleURIConnectionPool pool = pools.get(uri);
        if (pool == null) {
            SimpleConnectionMaker undertowConnectionMaker = SimpleClientConnectionMaker.instance();
            ClientConfig config = ClientConfig.get();
            pool = new SimpleURIConnectionPool(uri, config.getConnectionExpireTime(), config.getConnectionPoolSize(),
                null, worker, bufferPool, ssl, options, undertowConnectionMaker);
            pools.putIfAbsent(uri, pool);
            pool = pools.get(uri);
        }

        int warmUpSize = requestConfig.getPoolWarmUpSize();
        long connectTimeout = requestConfig.getConnectTimeout();
        return pool.warmUp(warmUpSize, connectTimeout);
    }

    /**
     * Simplified warm-up method using default worker and buffer pool.
     *
     * @param uri the URI to warm up connections for
     * @return the number of connections actually created
     */
    public int warmUpPool(URI uri) {
        XnioSsl ssl = HTTPS.equals(uri.getScheme()) ? getDefaultXnioSsl() : null;
        OptionMap options = ClientConfig.get().getRequestEnableHttp2()
            ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)
            : OptionMap.EMPTY;
        return warmUpPool(uri, WORKER, ssl, BUFFER_POOL, options);
    }

    /**
     * Shutdown the Http2Client, stopping background threads like the health checker.
     * Call this when the application is shutting down.
     */
    public void shutdown() {
        if (healthChecker != null) {
            healthChecker.stop();
            logger.info("Connection health checker stopped");
        }
    }

    /**
     * Get the connection pool map for a specific URI.
     * Useful for debugging and monitoring.
     * @return unmodifiable map of URI to connection pool
     */
    public Map<URI, SimpleURIConnectionPool> getPools() {
        return Collections.unmodifiableMap(pools);
    }

    /**
     * Connects to the given URI with SSL.
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        return connect((InetSocketAddress) null, uri, worker, ssl, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI, worker, and SSL, specifying if it is HTTP/2.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param isHttp2 true if HTTP/2
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, boolean isHttp2) {
        if(logger.isDebugEnabled()) logger.debug("The connection is http2?:" + isHttp2);
        return borrowConnection(uri, worker, ssl, bufferPool, isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);

    }
    /**
     * Borrows a connection for the given URI, worker, SSL, and options.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        return connect((InetSocketAddress) null, uri, worker, ssl, bufferPool, options);
    }

    /**
     * Borrows a connection token from the pool for the given URI, worker, SSL, and options.
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return a ConnectionToken
     */
    public SimpleConnectionHolder.ConnectionToken borrow(final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if(HTTPS.equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        SimpleURIConnectionPool pool = pools.get(uri);
        if(pool == null) {
            SimpleConnectionMaker undertowConnectionMaker = SimpleClientConnectionMaker.instance();
            ClientConfig config = ClientConfig.get();
            pool = new SimpleURIConnectionPool(uri, config.getConnectionExpireTime(), config.getConnectionPoolSize(), null, worker, bufferPool, ssl, options, undertowConnectionMaker);
            pools.putIfAbsent(uri, pool);
        }
        return pool.borrow(ClientConfig.get().getRequest().getConnectTimeout());
    }

    /**
     * Borrows a connection for the given URI, worker, and SSL, specifying if it is HTTP/2, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param isHttp2 true if HTTP/2
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, boolean isHttp2) {
        IoFuture<ClientConnection> future = borrowConnection(uri, worker, ssl, bufferPool, isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
        return getFutureConnection(timeoutSeconds, future);
    }
    /**
     * Borrows a connection for the given URI, worker, SSL, and options, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        IoFuture<ClientConnection> future = borrowConnection(uri, worker, ssl, bufferPool, options);
        return getFutureConnection(timeoutSeconds, future);
    }
    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        final FutureResult<ClientConnection> result = new FutureResult<>();
        provider.connect(new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection r) {
                if(logger.isDebugEnabled()) logger.debug("Adding the new connection: {} to FutureResult and cache it for uri: {}", r, uri);
                result.setResult(r);
                http2ClientConnectionPool.cacheConnection(uri, r);
            }

            @Override
            public void failed(IOException e) {
                if(logger.isDebugEnabled()) logger.debug("Failed to get new connection for uri: {}", uri);
                result.setException(e);
            }
        }, bindAddress, uri, worker, ssl, bufferPool, options);
        return result.getIoFuture();
    }
    /**
     * Connects to the given URI using the given IoThread, buffer pool, and options.
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        return connect((InetSocketAddress) null, uri, ioThread, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI, IoThread, and options.
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        return connect((InetSocketAddress) null, uri, ioThread, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI, IoThread, and options, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        IoFuture<ClientConnection> future = borrowConnection(uri, ioThread, bufferPool, options);
        return getFutureConnection(timeoutSeconds, future);
    }
    /**
     * Borrows a connection for the given URI, IoThread, and buffer pool, specifying if it is HTTP/2, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param isHttp2 true if HTTP/2
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, boolean isHttp2) {
        IoFuture<ClientConnection> future = borrowConnection(uri, ioThread, bufferPool, isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
        return getFutureConnection(timeoutSeconds, future);
    }
    /**
     * Connects to the given URI with a bind address, using the given IoThread, buffer pool, and options.
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        return connect(bindAddress, uri, ioThread, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI and IoThread with a bind address.
     * @param bindAddress the local address to bind to
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        return connect(bindAddress, uri, ioThread, null, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI and IoThread with a bind address and a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param bindAddress the local address to bind to
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        IoFuture<ClientConnection> future = borrowConnection(bindAddress, uri, ioThread, bufferPool, options);
        return getFutureConnection(timeoutSeconds, future);
    }
    /**
     * Connects to the given URI with SSL using the given IoThread.
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> connect(final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        return connect((InetSocketAddress) null, uri, ioThread, ssl, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI, IoThread, SSL, and options.
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    @Deprecated
    public IoFuture<ClientConnection> borrowConnection(final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientConnection connection = http2ClientConnectionPool.getConnection(uri);
        if(connection != null && connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Got an open connection from http2ClientConnectionPool");
            result.setResult(connection);
            return result.getIoFuture();
        }
        if(logger.isDebugEnabled()) logger.debug("Got a null or non open connection: {} from http2ClientConnectionPool. Creating a new one ...", connection);
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        return connect((InetSocketAddress) null, uri, ioThread, ssl, bufferPool, options);
    }
    /**
     * Borrows a connection for the given URI, IoThread, SSL, and options, with a timeout.
     * @param timeoutSeconds timeout in seconds
     * @param uri the URI of the service
     * @param ioThread the Xnio IoThread
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return ClientConnection object
     */
    @Deprecated
    public ClientConnection borrowConnection(long timeoutSeconds, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        IoFuture<ClientConnection> future = borrowConnection(uri, ioThread, ssl, bufferPool, options);
        return getFutureConnection(timeoutSeconds, future);
    }
    /**
     * Connects to the given URI using the given IoThread, SSL, and options.
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return an IoFuture that will contain the ClientConnection
     */
    public IoFuture<ClientConnection> connect(InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        final FutureResult<ClientConnection> result = new FutureResult<>();
        provider.connect(new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection r) {
                if(logger.isDebugEnabled()) logger.debug("Adding the new connection: {} to FutureResult and cache it for uri: {}", r, uri);
                result.setResult(r);
                http2ClientConnectionPool.cacheConnection(uri, r);
            }

            @Override
            public void failed(IOException e) {
                if(logger.isDebugEnabled()) logger.debug("Failed to get new connection for uri: {}", uri);
                result.setException(e);
            }
        }, bindAddress, uri, ioThread, ssl, bufferPool, options);
        return result.getIoFuture();
    }
    /**
     * Connects to the given URI with a listener, using the given worker, buffer pool, and options.
     * @param listener the callback to notify
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, uri, worker, null, bufferPool, options);
    }
    /**
     * Connects to the given URI with a listener and a bind address, using the given worker, buffer pool, and options.
     * @param listener the callback to notify
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, bindAddress, uri, worker, null, bufferPool, options);
    }

    /**
     * Connects to the given URI with a listener, using the given worker, SSL, buffer pool, and options.
     * @param listener the callback to notify
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, uri, worker, ssl, bufferPool, options);
    }

    /**
     * Connects to the given URI with a listener, using the given worker, SSL, and options.
     * @param listener the callback to notify when the connection is established or fails
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, bindAddress, uri, worker, ssl, bufferPool, options);
    }
    /**
     * Connects to the given URI with a listener, using the given IoThread, buffer pool, and options.
     * @param listener the callback to notify
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, uri, ioThread, null, bufferPool, options);
    }


    /**
     * Connects to the given URI with a listener and a bind address, using the given IoThread, buffer pool, and options.
     * @param listener the callback to notify
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, ByteBufferPool bufferPool, OptionMap options) {
        connect(listener, bindAddress, uri, ioThread, null, bufferPool, options);
    }

    /**
     * Connects to the given URI with a listener, using the given IoThread, SSL, buffer pool, and options.
     * @param listener the callback to notify
     * @param uri the URI to connect to
     * @param ioThread the Xnio IoThread
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     */
    @Deprecated
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, uri, ioThread, ssl, bufferPool, options);
    }

    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && ssl == null) ssl = getDefaultXnioSsl();
        ClientProvider provider = getClientProvider(uri);
        provider.connect(listener, bindAddress, uri, ioThread, ssl, bufferPool, options);
    }

    private ClientProvider getClientProvider(URI uri) {
        return clientProviders.get(uri.getScheme());
    }

    /**
     * Returns the singleton instance of Http2Client.
     * @return the singleton instance
     */
    public static Http2Client getInstance() {
        if (INSTANCE == null || clientConfig != ClientConfig.get()) {
            synchronized (Http2Client.class) {
                if (INSTANCE == null || clientConfig != ClientConfig.get()) {
                    clientConfig = ClientConfig.get();
                    INSTANCE = new Http2Client();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns a new instance of Http2Client with the given class loader.
     * @param classLoader the class loader to use
     * @return a new instance
     */
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
     * Add Authorization Code grant token the caller app gets from OAuth2 server and inject OpenTracing context
     *
     * This is the method called from client like web server that want to have Tracer context pass through.
     *
     * @param request the http request
     * @param token the bearer token
     * @param tracer the OpenTracing tracer
     */
    /*
    public void addAuthTokenTrace(ClientRequest request, String token, Tracer tracer) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                // other cases of Bearer
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
        if(tracer != null && tracer.activeSpan() != null) {
            Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
            Tags.HTTP_METHOD.set(tracer.activeSpan(), request.getMethod().toString());
            Tags.HTTP_URL.set(tracer.activeSpan(), request.getPath());
            tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new ClientRequestCarrier(request));
        }
    }
    */

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
        Result<Jwt> result = tokenManager.getJwt(request.getPath(), request.getRequestHeaders().getFirst(ClientConfig.SCOPE), request.getRequestHeaders().getFirst(ClientConfig.SERVICE_ID));
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
        Result<Jwt> result = tokenManager.getJwt(request.getPath(), request.getRequestHeaders().getFirst(ClientConfig.SCOPE), request.getRequestHeaders().getFirst(ClientConfig.SERVICE_ID));
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
     * @return Result
     */
    public Result propagateHeaders(ClientRequest request, final HttpServerExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        return populateHeader(request, token, cid, tid);

        /*
        boolean injectOpenTracing = config.isInjectOpenTracing();
        if(injectOpenTracing) {
            Tracer tracer = exchange.getAttachment(AttachmentConstants.EXCHANGE_TRACER);
            return populateHeader(request, token, tracer);
        } else {
            String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
            String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
            return populateHeader(request, token, cid, tid);
        }
        */
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
        Result<Jwt> result = tokenManager.getJwt(request.getPath(), request.getRequestHeaders().getFirst(ClientConfig.SCOPE), request.getRequestHeaders().getFirst(ClientConfig.SERVICE_ID));
        if(result.isFailure()) { return Failure.of(result.getError()); }
        // we cannot assume that the authToken is passed from the original caller. If it is null, then promote.
        if(authToken == null) {
            authToken = "Bearer " + result.getResult().getJwt();
        } else {
            request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + result.getResult().getJwt());
        }
        request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, correlationId);
        if(traceabilityId != null) {
            addAuthTokenTrace(request, authToken, traceabilityId);
        } else {
            addAuthToken(request, authToken);
        }
        ClientConfig config = ClientConfig.get();
        if(config.isInjectCallerId()) {
            if ("unknown".equals(callerId)) {
                ServerConfig serverConfig = ServerConfig.getInstance();
                if(serverConfig != null) {
                    callerId = serverConfig.getServiceId();
                }
            }
            request.getRequestHeaders().put(HttpStringConstants.CALLER_ID, callerId);
        }
        return result;
    }

//    /**
//     * Support API to API calls with scope token. The token is the original token from consumer and
//     * the client credentials token of caller API is added from cache. This method doesn't have correlationId
//     * and traceabilityId but has a Tracer for OpenTracing context passing. For standalone client, you create
//     * the Tracer instance and in the service to service call, the Tracer can be found in the JaegerStartupHookProvider
//     *
//     * This method is used in API to API call
//     *
//     * @param request the http request
//     * @param authToken the authorization token
//     * @param tracer the OpenTracing Tracer
//     * @return Result when fail to get jwt, it will return a Status.
//     */
    /*
    public Result populateHeader(ClientRequest request, String authToken, Tracer tracer) {
        Result<Jwt> result = tokenManager.getJwt(request.getPath(), request.getRequestHeaders().getFirst(ClientConfig.SCOPE), request.getRequestHeaders().getFirst(ClientConfig.SERVICE_ID));
        if(result.isFailure()) { return Failure.of(result.getError()); }
        // we cannot assume the original caller always has an authorization token. If authToken is null, then promote...
        if(authToken == null) {
            authToken = "Bearer " + result.getResult().getJwt();
        } else {
            request.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + result.getResult().getJwt());
        }
        if(tracer != null) {
            addAuthTokenTrace(request, authToken, tracer);
        } else {
            addAuthToken(request, authToken);
        }
        return result;
    }
    */


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
     * create ssl context using specified trustedName config
     *
     * @return SSLContext
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
	public static SSLContext createSSLContext() throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
        if(tlsMap != null) {
            try {
                // load key store for client certificate if two way ssl is used.
                Boolean loadKeyStore = tlsMap.get(LOAD_KEY_STORE) == null ? false : Config.loadBooleanValue(LOAD_KEY_STORE, tlsMap.get(LOAD_KEY_STORE));
                if (loadKeyStore != null && loadKeyStore) {
                    String keyStoreName = System.getProperty(KEY_STORE_PROPERTY);
                    String keyStorePass = System.getProperty(KEY_STORE_PASSWORD_PROPERTY);
                    if (keyStoreName != null && keyStorePass != null) {
                        if(logger.isInfoEnabled()) logger.info("Loading key store from system property at " + Encode.forJava(keyStoreName));
                    } else {
                        keyStoreName = (String) tlsMap.get(KEY_STORE);
                        keyStorePass = (String) tlsMap.get(KEY_STORE_PASS);
                        if(keyStorePass == null) {
                            logger.error(new Status(CONFIG_PROPERTY_MISSING, KEY_STORE_PASS, "client.yml").toString());
                        }
                        if(logger.isInfoEnabled()) logger.info("Loading key store from config at " + Encode.forJava(keyStoreName));
                    }
                    if (keyStoreName != null && keyStorePass != null) {
                        String keyPass = (String) tlsMap.get(KEY_PASS);
                        if(keyPass == null) {
                            logger.error(new Status(CONFIG_PROPERTY_MISSING, KEY_PASS, "client.yml").toString());
                        }
                        KeyStore keyStore = TlsUtil.loadKeyStore(keyStoreName, keyStorePass.toCharArray());
                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        keyManagerFactory.init(keyStore, keyPass.toCharArray());
                        keyManagers = keyManagerFactory.getKeyManagers();
                    }
                }
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
                throw new IOException("Unable to initialise KeyManager[]", e);
            }

            TrustManager[] trustManagers = null;
            Boolean loadDefaultTrust = tlsMap.get(LOAD_DEFAULT_TRUST) == null ? false : Config.loadBooleanValue(LOAD_DEFAULT_TRUST, tlsMap.get(LOAD_DEFAULT_TRUST));
            List<TrustManager> trustManagerList = new ArrayList<>();
            try {
                // load trust store, this is the server's public key certificate
                // first check if javax.net.ssl.trustStore system properties is set. It is only necessary if the server
                // certificate doesn't have the entire chain.
                Boolean loadTrustStore = tlsMap.get(LOAD_TRUST_STORE) == null ? false : Config.loadBooleanValue(LOAD_TRUST_STORE, tlsMap.get(LOAD_TRUST_STORE));
                if (loadTrustStore != null && loadTrustStore) {

                    String trustStoreName = (String) tlsMap.get(TRUST_STORE);
                    String trustStorePass = (String) tlsMap.get(TRUST_STORE_PASS);
                    if(trustStorePass == null) {
                        logger.error(new Status(CONFIG_PROPERTY_MISSING, TRUST_STORE_PASS, "client.yml").toString());
                    }
                    if(logger.isInfoEnabled()) logger.info("Loading trust store from config at " + Encode.forJava(trustStoreName));
                    if (trustStoreName != null && trustStorePass != null) {
                        KeyStore trustStore = TlsUtil.loadKeyStore(trustStoreName, trustStorePass.toCharArray());
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init(trustStore);
                        trustManagers = trustManagerFactory.getTrustManagers();
                        // the client.truststore is loaded.
                    }
                    if (loadDefaultTrust != null && loadDefaultTrust) {
                        TrustManager[] defaultTrusts = loadDefaultTrustStore();
                        if (defaultTrusts!=null && defaultTrusts.length>0) {
                            trustManagerList.addAll(Arrays.asList(defaultTrusts));
                        }
                    }
                    if (trustManagers!=null && trustManagers.length>0) {
                        trustManagerList.addAll(Arrays.asList(trustManagers));
                    }
                }
            } catch (Exception e) {
                throw new IOException("Unable to initialise TrustManager[]", e);
            }

            try {
                String tlsVersion = (String)tlsMap.get(TLS_VERSION);
                if(tlsVersion == null) tlsVersion = "TLSv1.2";
                sslContext = SSLContext.getInstance(tlsVersion);
                if (loadDefaultTrust != null && loadDefaultTrust && !trustManagerList.isEmpty()) {
                    TrustManager[] compositeTrustManagers = {new CompositeX509TrustManager(convertTrustManagers(trustManagerList))};
                    sslContext.init(keyManagers, compositeTrustManagers, null);
                } else {
                    if(trustManagers == null || trustManagers.length == 0) {
                        logger.error("No trust store is loaded. Please check client.yml");
                    } else {
                        TrustManager[] extendedTrustManagers = {new ClientX509ExtendedTrustManager(trustManagerList)};
                        sslContext.init(keyManagers, extendedTrustManagers, null);
                    }
                }
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Unable to create and initialise the SSLContext", e);
            }
        } else {
            logger.error("TLS configuration section is missing in client.yml");
        }

        return sslContext;
    }
    /**
     * Converts a list of TrustManagers to a list of X509TrustManagers.
     * @param trustManagerList the list of TrustManagers
     * @return the list of X509TrustManagers
     */
    public static List<X509TrustManager> convertTrustManagers(List<TrustManager> trustManagerList) {
        List<X509TrustManager> x509TrustManagers = new ArrayList<>();
        for (TrustManager trustManager : trustManagerList) {
            if (trustManager instanceof X509TrustManager) {
                x509TrustManagers.add((X509TrustManager) trustManager);
            }
        }
        return x509TrustManagers;
    }

    /**
     * Loads the default trust store.
     * @return an array of TrustManagers
     * @throws Exception if an error occurs during loading
     */
    public  static  TrustManager[] loadDefaultTrustStore() throws Exception {
        Path location = null;
        String password = "changeit"; //default value for cacerts, we can override it from config
        TlsConfig tlsConfig = ClientConfig.get().getTls();
        if(tlsConfig != null &&  tlsConfig.getDefaultCertPassword() != null) {
            password = String.valueOf(tlsConfig.getDefaultCertPassword());
        }
        String locationProperty = System.getProperty(TRUST_STORE_PROPERTY);
        if (!StringUtils.isEmpty(locationProperty)) {
            Path p = Paths.get(locationProperty);
            File f = p.toFile();
            if (f.exists() && f.isFile() && f.canRead()) {
                location = p;
            }
        }  else {
            String javaHome = System.getProperty("java.home");
            location = Paths.get(javaHome, "lib", "security", "jssecacerts");
            if (!location.toFile().exists()) {
                location = Paths.get(javaHome, "lib", "security", "cacerts");
            }
        }
        if (!location.toFile().exists()) {
            logger.warn("Cannot find system default trust store");
            return null;
        }

        String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
        if (!StringUtils.isEmpty(trustStorePass)) {
            password = trustStorePass;
        }
        String trustStoreType = System.getProperty(TRUST_STORE_TYPE_PROPERTY);
        String type;
        if (!StringUtils.isEmpty(trustStoreType)) {
            type = trustStoreType;
        } else {
            type = KeyStore.getDefaultType();
        }
        KeyStore trustStore = KeyStore.getInstance(type, Security.getProvider("SUN"));
        try (InputStream is = Files.newInputStream(location)) {
            trustStore.load(is, password.toCharArray());
            logger.info("JDK default trust store loaded from : {} .", location );
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();

    }

    /**
     * Returns a form data string from a map of parameters.
     * @param params the parameters
     * @return the form data string
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
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

    /**
     * Creates a client callback for the given reference and latch.
     * @param reference the response reference
     * @param latch the countdown latch
     * @return the client callback
     */
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
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Service call response = {}", string);
                                }
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

    /**
     * Creates a ClientCallback for ClientExchange that buffers the response body as a ByteBuffer.
     * @param reference the atomic reference to store the ClientResponse
     * @param latch the latch to signal when the request is complete
     * @return a ClientCallback for ClientExchange
     */
    public ClientCallback<ClientExchange> byteBufferClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch) {
        return new ClientCallback<ClientExchange>() {
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    public void completed(final ClientExchange result) {
                        reference.set(result.getResponse());
                        (new ByteBufferReadChannelListener(result.getConnection().getBufferPool()) {
                            protected void bufferDone(List<Byte> out) {
                                byte[] byteArray = new byte[out.size()];
                                int index = 0;
                                for (byte b : out) {
                                    byteArray[index++] = b;
                                }
                                result.getResponse().putAttachment(BUFFER_BODY, (ByteBuffer.wrap(byteArray)));
                                latch.countDown();
                            }

                            protected void error(IOException e) {
                                latch.countDown();
                            }
                        }).setup(result.getResponseChannel());
                    }
                    public void failed(IOException e) {
                        latch.countDown();
                    }
                });

                try {
                    result.getRequestChannel().shutdownWrites();
                    if (!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.flushingChannelListener((ChannelListener)null, (ChannelExceptionHandler)null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException var3) {
                    latch.countDown();
                }

            }

            public void failed(IOException e) {
                latch.countDown();
            }
        };
    }

    /**
     * Creates a ClientCallback for ClientExchange that buffers the response body as a ByteBuffer,
     * and sends the given request body.
     * @param reference the atomic reference to store the ClientResponse
     * @param latch the latch to signal when the request is complete
     * @param requestBody the request body to send
     * @return a ClientCallback for ClientExchange
     */
    public ClientCallback<ClientExchange> byteBufferClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch, final ByteBuffer requestBody) {
        return new ClientCallback<ClientExchange>() {
            public void completed(ClientExchange result) {
                new ByteBufferWriteChannelListener(requestBody).setup(result.getRequestChannel());
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    public void completed(final ClientExchange result) {
                        reference.set(result.getResponse());
                        (new ByteBufferReadChannelListener(result.getConnection().getBufferPool()) {
                            protected void bufferDone(List<Byte> out) {
                                byte[] byteArray = new byte[out.size()];
                                int index = 0;
                                for (byte b : out) {
                                    byteArray[index++] = b;
                                }
                                result.getResponse().putAttachment(BUFFER_BODY, (ByteBuffer.wrap(byteArray)));
                                latch.countDown();
                            }

                            protected void error(IOException e) {
                                latch.countDown();
                            }
                        }).setup(result.getResponseChannel());
                    }
                    public void failed(IOException e) {
                        latch.countDown();
                    }
                });

                try {
                    result.getRequestChannel().shutdownWrites();
                    if (!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.flushingChannelListener((ChannelListener)null, (ChannelExceptionHandler)null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException var3) {
                    latch.countDown();
                }

            }

            public void failed(IOException e) {
                latch.countDown();
            }
        };
    }

    /**
     * Creates a client callback for the given reference, latch, and request body.
     * @param reference the response reference
     * @param latch the countdown latch
     * @param requestBody the request body
     * @return the client callback
     */
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
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Service call response = {}", string);
                                }
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

    /**
     * Creates a full callback for the given reference and latch.
     * @param reference the response reference
     * @param latch the countdown latch
     * @return the client callback
     */
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
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Service call response = {}", string);
                                }
                                AsyncResponse ar = new AsyncResponse(result.getResponse(), string, System.currentTimeMillis() - startTime);
                                reference.set(DefaultAsyncResult.succeed(ar));
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                logger.error("IOException:", e);
                                reference.set(DefaultAsyncResult.fail(e));
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        logger.error("IOException:", e);
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
                    logger.error("IOException:", e);
                    reference.set(DefaultAsyncResult.fail(e));
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                logger.error("IOException:", e);
                reference.set(DefaultAsyncResult.fail(e));
                latch.countDown();
            }
        };
    }

    /**
     * Creates a full callback for the given reference, latch, and request body.
     * @param reference the response reference
     * @param latch the countdown latch
     * @param requestBody the request body
     * @return the client callback
     */
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
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Service call response = {}", string);
                                }
                                AsyncResponse ar = new AsyncResponse(result.getResponse(), string, System.currentTimeMillis() - startTime);
                                reference.set(DefaultAsyncResult.succeed(ar));
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                logger.error("IOException:", e);
                                reference.set(DefaultAsyncResult.fail(e));
                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        logger.error("IOException:", e);
                        reference.set(DefaultAsyncResult.fail(e));
                        latch.countDown();
                    }
                });
            }

            @Override
            public void failed(IOException e) {
                logger.error("IOException:", e);
                reference.set(DefaultAsyncResult.fail(e));
                latch.countDown();
            }
        };
    }

    /**
     * Returns a circuit breaker for the given request.
     * @param uri the URI
     * @param request the request
     * @param requestBody the request body
     * @return the circuit breaker
     */
    public CircuitBreaker getRequestService(URI uri, ClientRequest request, Optional<String> requestBody) {
        return new CircuitBreaker(() -> callService(uri, request, requestBody));
    }

    /**
     * Returns a circuit breaker for the given request, specifying if it is HTTP/2.
     * @param uri the URI
     * @param request the request
     * @param requestBody the request body
     * @param isHttp2 true if HTTP/2
     * @return the circuit breaker
     */
    public CircuitBreaker getRequestService(URI uri, ClientRequest request, Optional<String> requestBody, boolean isHttp2) {
        return new CircuitBreaker(() -> callService(uri, request, requestBody, isHttp2));
    }

    /**
     * This method is used to call the service corresponding to the uri and obtain a response, connection pool is embedded.
     * @param uri URI of target service
     * @param request request
     * @param requestBody request body
     * @return client response
     */
    public CompletableFuture<ClientResponse> callService(URI uri, ClientRequest request, Optional<String> requestBody) {
        addHostHeader(request);
        CompletableFuture<ClientResponse> futureClientResponse;
        AtomicReference<ClientConnection> currentConnection = new AtomicReference<>(http2ClientConnectionPool.getConnection(uri));
        if (currentConnection.get() != null && currentConnection.get().isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Reusing the connection: {} to {}", currentConnection.toString(), uri.toString());
            futureClientResponse = getFutureClientResponse(currentConnection.get(), uri, request, requestBody);
        } else {
            CompletableFuture<ClientConnection> futureConnection = this.connectAsync(uri);
            futureClientResponse = futureConnection.thenComposeAsync(clientConnection -> {
                currentConnection.set(clientConnection);
                return getFutureClientResponse(clientConnection, uri, request, requestBody);
            });
        }
        futureClientResponse.thenAcceptAsync(clientResponse -> http2ClientConnectionPool.resetConnectionStatus(currentConnection.get()));
        return futureClientResponse;
    }

    /**
     * This method is used to call the service corresponding to the uri and obtain a response, connection pool is embedded.
     * @param uri URI of target service
     * @param request request
     * @param requestBody request body
     * @param isHttp2 indicate if client request is http2
     * @return client response
     */
    public CompletableFuture<ClientResponse> callService(URI uri, ClientRequest request, Optional<String> requestBody, boolean isHttp2) {
        addHostHeader(request);
        CompletableFuture<ClientResponse> futureClientResponse;
        AtomicReference<ClientConnection> currentConnection = new AtomicReference<>(http2ClientConnectionPool.getConnection(uri));
        if (currentConnection.get() != null && currentConnection.get().isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("Reusing the connection: {} to {}", currentConnection.toString(), uri.toString());
            futureClientResponse = getFutureClientResponse(currentConnection.get(), uri, request, requestBody);
        } else {
            CompletableFuture<ClientConnection> futureConnection = this.connectAsync(uri, isHttp2);
            futureClientResponse = futureConnection.thenComposeAsync(clientConnection -> {
                currentConnection.set(clientConnection);
                return getFutureClientResponse(clientConnection, uri, request, requestBody);
            });
        }
        futureClientResponse.thenAcceptAsync(clientResponse -> http2ClientConnectionPool.resetConnectionStatus(currentConnection.get()));
        return futureClientResponse;
    }

    /**
     * This method is used to call the service by using the serviceId and obtain a response
     * service discovery, load balancing and connection pool are embedded.
     * @param protocol target service protocol
     * @param serviceId target service's service Id
     * @param envTag environment tag
     * @param request request
     * @param requestBody request body
     * @return client response
     */
    public CompletableFuture<ClientResponse> callService(String protocol, String serviceId, String envTag, ClientRequest request, Optional<String> requestBody) {
        try {
            Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
            String url = cluster.serviceToUrl(protocol, serviceId, envTag, null);
            if (url == null) {
                logger.error("Failed to discover service with serviceID: {}, and tag: {}", serviceId, envTag);
                throw new ClientException(String.format("Failed to discover service with serviceID: %s, and tag: %s", serviceId, envTag));
            }
            return callService(new URI(url), request, requestBody);
        } catch (Exception e) {
            logger.error("Failed to call service: {}", serviceId);
            throw new RuntimeException("Failed to call service: " + serviceId, e);
        }
    }

    /**
     * This method is used to call the service by using the serviceId and obtain a response
     * service discovery, load balancing and connection pool are embedded.
     * @param protocol target service protocol
     * @param serviceId target service's service Id
     * @param envTag environment tag
     * @param request request
     * @param requestBody request body
     * @param isHttp2 indicate if client request is http2
     * @return client response
     */
    public CompletableFuture<ClientResponse> callService(String protocol, String serviceId, String envTag, ClientRequest request, Optional<String> requestBody, boolean isHttp2) {
        try {
            Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
            String url = cluster.serviceToUrl(protocol, serviceId, envTag, null);
            if (url == null) {
                logger.error("Failed to discover service with serviceID: {}, and tag: {}", serviceId, envTag);
                throw new ClientException(String.format("Failed to discover service with serviceID: %s, and tag: %s", serviceId, envTag));
            }
            return callService(new URI(url), request, requestBody, isHttp2);
        } catch (Exception e) {
            logger.error("Failed to call service: {}", serviceId);
            throw new RuntimeException("Failed to call service: " + serviceId, e);
        }
    }

    /**
     * Create async connection with default config value
     * @param uri URI
     * @return CompletableFuture
     */
    public CompletableFuture<ClientConnection> connectAsync(URI uri) {
        if("https".equals(uri.getScheme()) && SSL == null) SSL = getDefaultXnioSsl();
        return this.connectAsync(null, uri, WORKER, SSL, com.networknt.client.Http2Client.BUFFER_POOL,
                ClientConfig.get().getRequestEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
    }

    /**
     * Create async connection with default config value
     * @param uri URI
     * @param isHttp2 indicate if it is https2 request
     * @return CompletableFuture
     */
    public CompletableFuture<ClientConnection> connectAsync(URI uri, boolean isHttp2) {
        if("https".equals(uri.getScheme()) && SSL == null) SSL = getDefaultXnioSsl();
        return this.connectAsync(null, uri, WORKER, SSL, com.networknt.client.Http2Client.BUFFER_POOL,
                isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY);
    }

    /**
     * Connects asynchronously to the given URI with the specified parameters.
     * @param bindAddress the local address to bind to
     * @param uri the URI to connect to
     * @param worker the Xnio worker
     * @param ssl the Xnio SSL context
     * @param bufferPool the buffer pool
     * @param options the option map
     * @return a CompletableFuture that will contain the ClientConnection
     */
    public CompletableFuture<ClientConnection> connectAsync(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        if("https".equals(uri.getScheme()) && SSL == null) SSL = getDefaultXnioSsl();
        CompletableFuture<ClientConnection> completableFuture = new CompletableFuture<>();
            ClientProvider provider = clientProviders.get(uri.getScheme());
            try {
                provider.connect(new ClientCallback<ClientConnection>() {
                    @Override
                    public void completed(ClientConnection r) {
                        completableFuture.complete(r);
                        http2ClientConnectionPool.cacheConnection(uri, r);
                    }

                    @Override
                    public void failed(IOException e) {
                        completableFuture.completeExceptionally(e);
                    }
                }, bindAddress, uri, worker, ssl, bufferPool, options);
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        return completableFuture;
    }

    private CompletableFuture<ClientResponse> getFutureClientResponse(ClientConnection clientConnection, URI uri, ClientRequest request, Optional<String> requestBody) {
        if (requestBody.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("The request sent to {} = request header: {}, request body: {}", uri.toString(), request.getRequestHeaders().toString(), requestBody.get());
            }
            Http2ClientCompletableFutureWithRequest futureClientResponseWithRequest = new Http2ClientCompletableFutureWithRequest(requestBody.get());
            try {
                clientConnection.sendRequest(request, futureClientResponseWithRequest);
            } catch (Exception e) {
                futureClientResponseWithRequest.completeExceptionally(e);
            }
            return futureClientResponseWithRequest;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("The request sent to {} = request header: {}, request body is empty", uri.toString(), request.getRequestHeaders().toString());
            }
            Http2ClientCompletableFutureNoRequest futureClientResponseNoRequest = new Http2ClientCompletableFutureNoRequest();
            try {
                clientConnection.sendRequest(request, futureClientResponseNoRequest);
            } catch (Exception e) {
                futureClientResponseNoRequest.completeExceptionally(e);
            }
            return futureClientResponseNoRequest;
        }
    }

    private void addHostHeader(ClientRequest request) {
        if (!request.getRequestHeaders().contains(Headers.HOST)) {
            request.getRequestHeaders().put(Headers.HOST, "localhost");
        }
    }
}
