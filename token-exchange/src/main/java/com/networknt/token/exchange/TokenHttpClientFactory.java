package com.networknt.token.exchange;

import com.networknt.client.ClientConfig;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.token.exchange.exception.TokenRequestException;
import com.networknt.token.exchange.schema.RequestSchema;
import com.networknt.token.exchange.schema.cert.SSLContextSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * Handles creation and caching of HTTP clients and requests for token services.
 * Each schema can have its own proxy, SSL, and HTTP/2 settings.
 */
public class TokenHttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TokenHttpClientFactory.class);

    private final TokenKeyStoreManager keyStoreManager;
    private final String defaultProxyHost;
    private final int defaultProxyPort;


    public TokenHttpClientFactory(final TokenKeyStoreManager keyStoreManager, final String defaultProxyHost, final int defaultProxyPort) {
        this.keyStoreManager = keyStoreManager;
        this.defaultProxyHost = defaultProxyHost;
        this.defaultProxyPort = defaultProxyPort;
    }

    /**
     * Gets or creates an HTTP client for the given request schema.
     * The client is cached per schema if cacheHttpClient is enabled.
     * Thread-safe through synchronized block on the schema's dedicated lock.
     */
    public HttpClient getOrCreateClient(final RequestSchema schema) {
        synchronized (schema.getClientLock()) {
            if (schema.getHttpClient() != null && schema.isCacheHttpClient()) {
                LOG.trace("Reusing cached HTTP client");
                return schema.getHttpClient();
            }

            LOG.debug("Creating new HTTP client for {}", schema.getUrl());
            final var client = createClient(schema);

            if (schema.isCacheHttpClient()) {
                schema.setHttpClient(client);
            }
            return client;
        }
    }

    /**
     * Builds an HTTP request for the given schema and resolved parameters.
     * Note: Requests are NOT cached because headers and body may contain dynamic
     * !ref() variables that change between calls (e.g., accessToken, constructedJwt).
     * Each token request should be built fresh with the current resolved values.
     */
    public HttpRequest buildRequest(
            final RequestSchema schema,
            final Map<String, String> resolvedHeaders,
            final Map<String, String> resolvedBody
    ) {
        LOG.debug("Building new HTTP request to {}", schema.getUrl());
        return new HttpTokenRequestBuilder(schema.getUrl())
                .withHeaders(resolvedHeaders)
                .withBody(resolvedBody, schema.getType())
                .build();
    }

    /**
     * Sends an HTTP request and returns the response.
     */
    public HttpResponse<String> send(final HttpClient client, final HttpRequest request) throws InterruptedException {
        try {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Response status: {}, body: {}", response.statusCode(), response.body());
            }
            return response;

        } catch (IOException e) {
            LOG.error("Failed to send request to {}: {}", request.uri(), e.getMessage());
            throw new TokenRequestException(request);
        }
    }

    private HttpClient createClient(final RequestSchema schema) {
        final var sslContext = createSSLContext(schema);
        final var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(ClientConfig.get().getRequest().getTimeout()))
                .sslContext(sslContext);

        // Use proxy settings from the schema (per-schema configuration)
        final var proxyHost = schema.getProxyHost();
        final var proxyPort = schema.getProxyPort();
        if (proxyHost != null && !proxyHost.isEmpty()) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(
                    proxyHost, proxyPort == 0 ? 443 : proxyPort)));
        } else if (this.defaultProxyHost != null && !this.defaultProxyHost.isEmpty()) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(
                    this.defaultProxyHost, this.defaultProxyPort == 0 ? 443 : this.defaultProxyPort
            )));
        }

        // Use HTTP/2 setting from the schema
        builder.version(schema.isEnableHttp2() ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1);
        configureHostnameVerification();
        return builder.build();
    }

    private SSLContext createSSLContext(final RequestSchema schema) {
        if (schema.getSslContextSchema() == null) {
            return createDefaultSSLContext();
        }

        if (schema.getSslContext() != null && schema.isCacheSSLContext()) {
            return schema.getSslContext();
        }

        LOG.debug("Creating custom SSL context");
        final var sslContext = createCustomSSLContext(schema.getSslContextSchema());
        schema.setSslContext(sslContext);
        return sslContext;
    }

    private SSLContext createDefaultSSLContext() {
        try {
            LOG.debug("Creating default SSL context from client.yml");
            return HttpClientRequest.createSSLContext();
        } catch (IOException e) {
            throw new RuntimeException("Could not create default SSL context", e);
        }
    }

    private SSLContext createCustomSSLContext(final SSLContextSchema sslSchema) {
        final var keyManagers = keyStoreManager.getKeyManagers(
                sslSchema.getKeyStore().getName(),
                sslSchema.getKeyStore().getPassword(),
                sslSchema.getKeyStore().getKeyPass(),
                sslSchema.getKeyStore().getAlgorithm()
        );

        final var trustManagers = keyStoreManager.getTrustManagers(
                sslSchema.getTrustStore().getName(),
                sslSchema.getTrustStore().getPassword(),
                sslSchema.getTrustStore().getAlgorithm()
        );

        if (trustManagers.length == 0) {
            throw new IllegalStateException("No trust managers found for SSL context");
        }

        try {
            final var sslContext = SSLContext.getInstance(sslSchema.getTlsVersion());
            final var extendedTrustManagers = new TrustManager[] {
                    new ClientX509ExtendedTrustManager(Arrays.asList(trustManagers))
            };
            sslContext.init(keyManagers, extendedTrustManagers, null);
            return sslContext;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("TLS version '" + sslSchema.getTlsVersion() + "' is not available", e);
        } catch (KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    private void configureHostnameVerification() {
        final var clientConfig = ClientConfig.get().getMappedConfig();
        if (clientConfig.get(ClientConfig.TLS) instanceof Map) {
            @SuppressWarnings("unchecked")
            final var tlsMap = (Map<String, Object>) clientConfig.get(ClientConfig.TLS);
            if (tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                final Properties props = System.getProperties();
                props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
            }
        }
    }
}
