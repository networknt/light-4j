/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client;

import com.networknt.client.oauth.ClientCredentialsRequest;
import com.networknt.client.oauth.TokenHelper;
import com.networknt.client.oauth.TokenRequest;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.status.Status;
import com.networknt.utility.*;
import io.undertow.server.HttpServerExchange;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String CONFIG_NAME = "client";
    public static final String CONFIG_SECRET = "secret";

    static final Logger logger = LoggerFactory.getLogger(Client.class);

    static final Integer DEFAULT_REACTOR_CONNECT_TIMEOUT = 10000;  // 10 seconds
    static final Integer DEFAULT_REACTOR_SO_TIMEOUT = 10000;       // 10 seconds

    static final String SYNC = "sync";
    static final String ASYNC = "async";
    static final String ROUTES = "routes";

    static final String MAX_CONNECTION_TOTAL = "maxConnectionTotal";
    static final String MAX_CONNECTION_PER_ROUTE = "maxConnectionPerRoute";
    static final String TIMEOUT = "timeout";
    static final String KEEP_ALIVE = "keepAlive";
    static final String TLS = "tls";
    static final String LOAD_TRUST_STORE = "loadTrustStore";
    static final String LOAD_KEY_STORE = "loadKeyStore";
    static final String VERIFY_HOSTNAME = "verifyHostname";
    static final String TRUST_STORE = "trustStore";
    static final String CLIENT_TRUSTSTORE_PASS = "clientTruststorePass";
    static final String KEY_STORE = "keyStore";
    static final String CLIENT_KEYSTORE_PASS = "clientKeystorePass";
    static final String CLIENT_KEY_PASS = "clientKeyPass";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";

    static final String REACTOR = "reactor";
    static final String REACTOR_IO_THREAD_COUNT = "ioThreadCount";
    static final String REACTOR_CONNECT_TIMEOUT = "connectTimeout";
    static final String REACTOR_SO_TIMEOUT = "soTimeout";

    static final String OAUTH = "oauth";
    static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";

    static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERR10009";

    static Map<String, Object> config;
    static Map<String, Object> oauthConfig;
    private volatile CloseableHttpClient httpClient = null;
    private volatile CloseableHttpAsyncClient httpAsyncClient = null;


    // Cached jwt token for this client.
    private String jwt;
    private long expire;
    private volatile boolean renewing = false;
    private volatile long expiredRetryTimeout;
    private volatile long earlyRetryTimeout;

    private final Object lock = new Object();

    static {
        List<String> masks = new ArrayList<>();
        ModuleRegistry.registerModule(Client.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), masks);
        config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        if(config != null) {
            oauthConfig = (Map<String, Object>)config.get(OAUTH);
        }
    }
    public static Map<String, Object> secret = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);

    // This eager initialization.
    private static final Client instance = new Client();

    // This is the only way to get instance
    public static Client getInstance() {
        return instance;
    }

    // private constructor to prevent create another instance.
    private Client() {}

    public CloseableHttpClient getSyncClient() throws ClientException {
        if(httpClient == null) {
            synchronized (Client.class) {
                if(httpClient == null) {
                    httpClient = httpClient();
                }
            }
        }
        return httpClient;
    }

    public CloseableHttpAsyncClient getAsyncClient() throws ClientException {
        if(httpAsyncClient == null) {
            synchronized (Client.class) {
                if(httpAsyncClient == null) {
                    httpAsyncClient = httpAsyncClient();
                }
            }
        }
        httpAsyncClient.start();
        return httpAsyncClient;
    }

    /**
     * Add Authorization Code grant token the caller app gets from OAuth2 server.
     *
     * This is the method called from client like web server
     *
     * @param request the http request
     * @param token the bearer token
     */
    public void addAuthToken(HttpRequest request, String token) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                // other cases of Bearer
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        request.addHeader(Constants.AUTHORIZATION, token);
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
    public void addAuthTokenTrace(HttpRequest request, String token, String traceabilityId) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                // other cases of Bearer
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        request.addHeader(Constants.AUTHORIZATION, token);
        request.addHeader(Constants.TRACEABILITY_ID, traceabilityId);
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
    public void addCcToken(HttpRequest request) throws ClientException, ApiException {
        checkCCTokenExpired();
        request.addHeader(Constants.AUTHORIZATION, "Bearer " + jwt);
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
    public void addCcTokenTrace(HttpRequest request, String traceabilityId) throws ClientException, ApiException {
        checkCCTokenExpired();
        request.addHeader(Constants.AUTHORIZATION, "Bearer " + jwt);
        request.addHeader(Constants.TRACEABILITY_ID, traceabilityId);
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
    public void propagateHeaders(HttpRequest request, final HttpServerExchange exchange) throws ClientException, ApiException {
        String tid = exchange.getRequestHeaders().getFirst(Constants.TRACEABILITY_ID);
        String token = exchange.getRequestHeaders().getFirst(Constants.AUTHORIZATION);
        String cid = exchange.getRequestHeaders().getFirst(Constants.CORRELATION_ID);
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
    public void populateHeader(HttpRequest request, String authToken, String correlationId, String traceabilityId) throws ClientException, ApiException {
        if(traceabilityId != null) {
            addAuthTokenTrace(request, authToken, traceabilityId);
        } else {
            addAuthToken(request, authToken);
        }
        request.addHeader(Constants.CORRELATION_ID, correlationId);
        checkCCTokenExpired();
        request.addHeader(Constants.SCOPE_TOKEN, "Bearer " + jwt);
    }

    private void getCCToken() throws ClientException {
        TokenRequest tokenRequest = new ClientCredentialsRequest();
        TokenResponse tokenResponse = TokenHelper.getToken(tokenRequest);
        synchronized (lock) {
            jwt = tokenResponse.getAccessToken();
            // the expiresIn is seconds and it is converted to millisecond in the future.
            expire = System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000;
            logger.info("Get client credentials token {} with expire_in {} seconds", jwt, tokenResponse.getExpiresIn());
        }
    }

    private void checkCCTokenExpired() throws ClientException, ApiException {
        long tokenRenewBeforeExpired = (Integer) oauthConfig.get(TOKEN_RENEW_BEFORE_EXPIRED);
        long expiredRefreshRetryDelay = (Integer)oauthConfig.get(EXPIRED_REFRESH_RETRY_DELAY);
        long earlyRefreshRetryDelay = (Integer)oauthConfig.get(EARLY_REFRESH_RETRY_DELAY);
        boolean isInRenewWindow = expire - System.currentTimeMillis() < tokenRenewBeforeExpired;
        logger.trace("isInRenewWindow = " + isInRenewWindow);
        if(isInRenewWindow) {
            if(expire <= System.currentTimeMillis()) {
                logger.trace("In renew window and token is expired.");
                // block other request here to prevent using expired token.
                synchronized (Client.class) {
                    if(expire <= System.currentTimeMillis()) {
                        logger.trace("Within the synch block, check if the current request need to renew token");
                        if(!renewing || System.currentTimeMillis() > expiredRetryTimeout) {
                            // if there is no other request is renewing or the renewing flag is true but renewTimeout is passed
                            renewing = true;
                            expiredRetryTimeout = System.currentTimeMillis() + expiredRefreshRetryDelay;
                            logger.trace("Current request is renewing token synchronously as token is expired already");
                            getCCToken();
                            renewing = false;
                        } else {
                            logger.trace("Circuit breaker is tripped and not timeout yet!");
                            // reject all waiting requests by thrown an exception.
                            throw new ApiException(new Status(STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE));
                        }
                    }
                }
            } else {
                // Not expired yet, try to renew async but let requests use the old token.
                logger.trace("In renew window but token is not expired yet.");
                synchronized (Client.class) {
                    if(expire > System.currentTimeMillis()) {
                        if(!renewing || System.currentTimeMillis() > earlyRetryTimeout) {
                            renewing = true;
                            earlyRetryTimeout = System.currentTimeMillis() + earlyRefreshRetryDelay;
                            logger.trace("Retrieve token async is called while token is not expired yet");

                            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                            executor.schedule(() -> {
                                try {
                                    getCCToken();
                                    renewing = false;
                                    logger.trace("Async get token is completed.");
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
        logger.trace("Check secondary token is done!");
    }


    private CloseableHttpClient httpClient() throws ClientException {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry());

        Map<String, Object> httpClientMap = (Map<String, Object>)config.get(SYNC);
        connectionManager.setMaxTotal((Integer)httpClientMap.get(MAX_CONNECTION_TOTAL));
        connectionManager.setDefaultMaxPerRoute((Integer) httpClientMap.get(MAX_CONNECTION_PER_ROUTE));
        // Now handle all the specific route defined.
        Map<String, Object> routeMap = (Map<String, Object>)httpClientMap.get(ROUTES);
        for (String route : routeMap.keySet()) {
            Integer maxConnection = (Integer) routeMap.get(route);
            connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(
                    route)), maxConnection);
        }
        final int timeout = (Integer)httpClientMap.get(TIMEOUT);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        final long keepAliveMilliseconds = (Integer)httpClientMap.get(KEEP_ALIVE);
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy((response, context) -> {
                    HeaderElementIterator it1 = new BasicHeaderElementIterator
                            (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                    while (it1.hasNext()) {
                        HeaderElement he = it1.nextElement();
                        String param = he.getName();
                        String value = he.getValue();
                        if (value != null && param.equalsIgnoreCase
                                ("timeout")) {
                            try {
                                logger.trace("Use server timeout for keepAliveMilliseconds");
                                return Long.parseLong(value) * 1000;
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }
                    //logger.trace("Use keepAliveMilliseconds from config " + keepAliveMilliseconds);
                    return keepAliveMilliseconds;
                })
               .setDefaultRequestConfig(config)
               .build();
    }

    private CloseableHttpAsyncClient httpAsyncClient() throws ClientException {
        PoolingNHttpClientConnectionManager connectionManager = new PoolingNHttpClientConnectionManager(
                ioReactor(), asyncRegistry());
        Map<String, Object> asyncHttpClientMap = (Map<String, Object>)config.get(ASYNC);
        connectionManager.setMaxTotal((Integer)asyncHttpClientMap.get(MAX_CONNECTION_TOTAL));
        connectionManager.setDefaultMaxPerRoute((Integer) asyncHttpClientMap.get(MAX_CONNECTION_PER_ROUTE));
        // Now handle all the specific route defined.
        Map<String, Object> routeMap = (Map<String, Object>)asyncHttpClientMap.get(ROUTES);
        for (String route : routeMap.keySet()) {
            Integer maxConnection = (Integer) routeMap.get(route);
            connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(
                    route)), maxConnection);
        }
        final int timeout = (Integer) asyncHttpClientMap.get(TIMEOUT);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        final long keepAliveMilliseconds = (Integer)asyncHttpClientMap.get(KEEP_ALIVE);

        return HttpAsyncClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy((response, context) -> {
                    HeaderElementIterator it1 = new BasicHeaderElementIterator
                            (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                    while (it1.hasNext()) {
                        HeaderElement he = it1.nextElement();
                        String param = he.getName();
                        String value = he.getValue();
                        if (value != null && param.equalsIgnoreCase
                                ("timeout")) {
                            try {
                                logger.trace("Use server timeout for keepAliveMilliseconds");
                                return Long.parseLong(value) * 1000;
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }
                    //logger.trace("Use keepAliveMilliseconds from config " + keepAliveMilliseconds);
                    return keepAliveMilliseconds;
                })
                .setDefaultRequestConfig(config)
                .build();
    }

    private Registry<SchemeIOSessionStrategy> asyncRegistry() throws ClientException {
        // Allow TLSv1 protocol only
        Registry<SchemeIOSessionStrategy> registry;
        try {
            SSLIOSessionStrategy sslSessionStrategy = new SSLIOSessionStrategy (
                    sslContext(),
                    new String[] { "TLSv1" },
                    null,
                    hostnameVerifier());

            // Create a registry of custom connection session strategies for supported
            // protocol schemes.
            registry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", sslSessionStrategy)
                    .build();

        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException: ", e);
            throw new ClientException("NoSuchAlgorithmException: ", e);
        } catch (KeyManagementException e) {
            logger.error("KeyManagementException: ", e);
            throw new ClientException("KeyManagementException: ", e);
        } catch (IOException e) {
            logger.error("IOException: ", e);
            throw new ClientException("IOException: ", e);
        }
        return registry;
    }

    private ConnectingIOReactor ioReactor() throws ClientException {
        Map<String, Object> asyncMap = (Map)config.get(ASYNC);
        Map<String, Object> reactorMap = (Map)asyncMap.get(REACTOR);
        Integer ioThreadCount = (Integer)reactorMap.get(REACTOR_IO_THREAD_COUNT);
        IOReactorConfig.Builder builder = IOReactorConfig.custom();
        builder.setIoThreadCount(ioThreadCount == null? Runtime.getRuntime().availableProcessors(): ioThreadCount);
        Integer connectTimeout = (Integer)reactorMap.get(REACTOR_CONNECT_TIMEOUT);
        builder.setConnectTimeout(connectTimeout == null? DEFAULT_REACTOR_CONNECT_TIMEOUT: connectTimeout);
        Integer soTimeout = (Integer)reactorMap.get(REACTOR_SO_TIMEOUT);
        builder.setSoTimeout(soTimeout == null? DEFAULT_REACTOR_SO_TIMEOUT: soTimeout);
        ConnectingIOReactor reactor;
        try {
            reactor = new DefaultConnectingIOReactor(builder.build());
        } catch (IOReactorException e) {
            logger.error("IOReactorException: ", e);
            throw new ClientException("IOReactorException: ", e);
        }
        return reactor;
    }

    private SSLContext sslContext() throws ClientException, IOException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = null;
        Map<String, Object> tlsMap = (Map)config.get(TLS);
        if(tlsMap != null) {
            SSLContextBuilder builder = SSLContexts.custom();
            // load trust store, this is the server public key certificate
            // first check if javax.net.ssl.trustStore system properties is set. It is only necessary if the server
            // certificate doesn't have the entire chain.
            Boolean loadTrustStore = (Boolean) tlsMap.get(LOAD_TRUST_STORE);
            if (loadTrustStore != null && loadTrustStore) {
                String trustStoreName = System.getProperty(TRUST_STORE_PROPERTY);
                String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
                if(trustStoreName != null && trustStorePass != null) {
                    logger.info("Loading trust store from system property at " + Encode.forJava(trustStoreName));
                } else {
                    trustStoreName = (String)tlsMap.get(TRUST_STORE);
                    trustStorePass = (String)secret.get(CLIENT_TRUSTSTORE_PASS);
                    logger.info("Loading trust store from config at " + Encode.forJava(trustStoreName));
                }

                KeyStore trustStore;
                if(trustStoreName != null && trustStorePass != null) {
                    InputStream trustStream = Config.getInstance().getInputStreamFromFile(trustStoreName);
                    if(trustStream != null) {
                        try {
                            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            trustStore.load(trustStream, trustStorePass.toCharArray());
                            builder.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy());
                        } catch(CertificateException ce) {
                            logger.error("CertificateException: Unable to load trust store.", ce);
                            throw new ClientException("CertificateException: Unable to load trust store.", ce);
                        } catch(KeyStoreException kse) {
                            logger.error("KeyStoreException: Unable to load trust store.", kse);
                            throw new ClientException("KeyStoreException: Unable to load trust store.", kse);
                        } finally {
                            trustStream.close();
                        }
                    }
                }
            }

            // load key store for client certificate if two way ssl is used.
            Boolean loadKeyStore = (Boolean) tlsMap.get(LOAD_KEY_STORE);
            if (loadKeyStore != null && loadKeyStore) {
                String keyStoreName = (String)tlsMap.get(KEY_STORE);
                String keyStorePass = (String)secret.get(CLIENT_KEYSTORE_PASS);
                String keyPass = (String)secret.get(CLIENT_KEY_PASS);
                KeyStore keyStore;
                if(keyStoreName != null && keyStorePass != null) {
                    InputStream keyStream = Config.getInstance().getInputStreamFromFile(keyStoreName);
                    if(keyStream != null) {
                        try {
                            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            keyStore.load(keyStream, keyStorePass.toCharArray());
                            builder.loadKeyMaterial(keyStore, keyPass.toCharArray());
                        } catch (CertificateException ce) {
                            logger.error("CertificateException: Unable to load key store.", ce);
                            throw new ClientException("CertificateException: Unable to load key store.", ce);
                        } catch(KeyStoreException kse) {
                            logger.error("KeyStoreException: Unable to load key store.", kse);
                            throw new ClientException("KeyStoreException: Unable to load key store.", kse);
                        } catch (UnrecoverableKeyException uke) {
                            logger.error("UnrecoverableKeyException: Unable to load key store.", uke);
                            throw new ClientException("UnrecoverableKeyException: Unable to load key store.", uke);
                        } finally {
                            keyStream.close();
                        }
                    }
                }
            }
            sslContext = builder.build();
        }
        return sslContext;
    }

    private HostnameVerifier hostnameVerifier() {
        Map<String, Object> tlsMap = (Map)config.get(TLS);
        HostnameVerifier verifier = null;
        if(tlsMap != null) {
            Boolean verifyHostname = (Boolean) tlsMap.get(VERIFY_HOSTNAME);
            if (verifyHostname != null && !verifyHostname) {
                verifier = new NoopHostnameVerifier();
            } else {
                verifier = new DefaultHostnameVerifier();
            }
        }
        return verifier;
    }

    private Registry<ConnectionSocketFactory> registry() throws ClientException {
        Registry<ConnectionSocketFactory> registry;
        try {
            // Allow TLSv1 protocol only
            SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
                    sslContext(),
                    new String[] { "TLSv1" },
                    null,
                    hostnameVerifier());
            // Create a registry of custom connection factory
            registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", sslFactory)
                    .build();
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException: in registry", e);
            throw new ClientException("NoSuchAlgorithmException: in registry", e);
        } catch (KeyManagementException e) {
            logger.error("KeyManagementException: in registry", e);
            throw new ClientException("KeyManagementException: in registry", e);
        } catch (IOException e) {
            logger.error("IOException: in registry", e);
            throw new ClientException("IOException: in registry", e);
        }
        return registry;
    }

}
