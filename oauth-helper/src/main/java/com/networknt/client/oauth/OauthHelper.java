/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package com.networknt.client.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.networknt.client.ClientConfig;
import com.networknt.cluster.Cluster;
import com.networknt.common.ContentType;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.exception.ClientException;
import com.networknt.http.client.Headers;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.utility.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OauthHelper {
    private static final String BASIC = "Basic";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CODE = "code";
    private static final String USER_ID = "userId";
    private static final String USER_TYPE = "userType";
    private static final String ROLES = "roles";
    private static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    private static final String FAIL_TO_SEND_REQUEST = "ERR10051";
    private static final String GET_TOKEN_ERROR = "ERR10052";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String GET_TOKEN_TIMEOUT = "ERR10054";
    private static final String TLS_TRUSTSTORE_ERROR = "ERR10055";
    private static final String OAUTH_SERVER_URL_ERROR = "ERR10056";
    public static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERR10009";

    private static final Logger logger = LoggerFactory.getLogger(OauthHelper.class);

    private static HttpClient tokenClient = null;
    private static HttpClient introspectionClient = null;
    private static HttpClient signClient = null;
    private static HttpClient derefClient = null;

    /**
     * Get an access token from the token service. A Result of TokenResponse will be returned if the invocation is successfully.
     * Otherwise, a Result of Status will be returned.
     *
     * @param tokenRequest token request constructed from the client.yml token section.
     * @return Result of TokenResponse or error Status.
     */
    public static Result<TokenResponse> getTokenResult(TokenRequest tokenRequest) {
        return getTokenResult(tokenRequest, null);
    }

    /**
     * Get an access token from the token service. A Result of TokenResponse will be returned if the invocation is successfully.
     * Otherwise, a Result of Status will be returned.
     *
     * @param tokenRequest token request constructed from the client.yml token section.
     * @param envTag the environment tag from the server.yml for service lookup.
     * @return Result of TokenResponse or error Status.
     */
    public static Result<TokenResponse> getTokenResult(TokenRequest tokenRequest, String envTag) {
        // As the tokenClient will be reused frequently, we create it once and cache it.
        if(logger.isTraceEnabled()) logger.trace("tokenRequest = " + JsonMapper.toJson(tokenRequest));
        if(tokenClient == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(logger.isTraceEnabled()) logger.trace("proxyHost = " + tokenRequest.getProxyHost() + " proxyPort = " + tokenRequest.getProxyPort());
                if(!StringUtils.isBlank(tokenRequest.getProxyHost())) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(tokenRequest.getProxyHost(), tokenRequest.getProxyPort() == 0 ? 443 : tokenRequest.getProxyPort())));
                if (tokenRequest.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }

                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                tokenClient = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }
        try {
            String serverUrl = tokenRequest.getServerUrl();
            if(serverUrl == null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                tokenRequest.setServerUrl(cluster.serviceToUrl("https", tokenRequest.getServiceId(), envTag, null));
                if(logger.isTraceEnabled()) logger.trace("serviceUrl is null, discovered url = " + tokenRequest.getServerUrl());
            }
            if(tokenRequest.getServerUrl() == null) {
                if(logger.isTraceEnabled()) logger.trace("serviceUrl is empty and could not discovery serviceUrl. tokenRequest = " + JsonMapper.toJson(tokenRequest));
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "token"));
            }
            if(logger.isTraceEnabled()) logger.trace("token service url = " + serverUrl);
            IClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.CLIENT_CREDENTIAL_REQUEST_COMPOSER);
            final HttpRequest request = requestComposer.composeClientRequest(tokenRequest);
            CompletableFuture<HttpResponse<String>> response = tokenClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
            HttpHeaders headers = response.thenApply(HttpResponse::headers).get();
            return handleResponse(getContentTypeHeaders(headers), body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, tokenRequest.getServerUrl()));
        }
    }

    /**
     * Get a signed JWT token from token service to ensure that nobody can modify the payload when the token
     * is passed from service to service. Unlike the access JWT token, this token is ensure the data integrity
     * with signature.
     *
     * @param signRequest SignRequest that is constructed from the client.yml sign section
     * @return Result that contains TokenResponse or error status when failed.
     */
    public static Result<TokenResponse> getSignResult(SignRequest signRequest) {
        return getSignResult(signRequest, null);
    }

    /**
     * Get a signed JWT token from token service to ensure that nobody can modify the payload when the token
     * is passed from service to service. Unlike the access JWT token, this token is ensure the data integrity
     * with signature.
     *
     * @param signRequest SignRequest that is constructed from the client.yml sign section
     * @param envTag environment tag that is used for service lookup if serviceId is used.
     * @return Result that contains TokenResponse or error status when failed.
     */
    public static Result<TokenResponse> getSignResult(SignRequest signRequest, String envTag) {
        if(signClient == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(signRequest.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(signRequest.getProxyHost(), signRequest.getProxyPort() == 0 ? 443 : signRequest.getProxyPort())));
                if (signRequest.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }

                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                signClient = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }
        try {
            String serverUrl = signRequest.getServerUrl();
            if(serverUrl == null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                signRequest.setServerUrl(cluster.serviceToUrl("https", signRequest.getServiceId(), envTag, null));
            }
            if(signRequest.getServerUrl() == null) {
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "sign"));
            }
            Map<String, Object> map = new HashMap<>();
            map.put("expires", signRequest.getExpires());
            map.put("payload", signRequest.getPayload());
            String requestBody = Config.getInstance().getMapper().writeValueAsString(map);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .uri(URI.create(signRequest.getServerUrl() + signRequest.getUri()));
            if(signRequest.getClientId() != null && signRequest.getClientSecret() != null) {
                requestBuilder.setHeader(Headers.AUTHORIZATION_STRING, getBasicAuthHeader(signRequest.getClientId(), signRequest.getClientSecret()));
            }
            requestBuilder.setHeader(Headers.CONTENT_TYPE_STRING, "application/json");

            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response = signClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
            HttpHeaders headers = response.thenApply(HttpResponse::headers).get();
            return handleResponse(getContentTypeHeaders(headers), body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, signRequest.getServerUrl()));
        }
    }

    /**
     * Get an access token from the token service based on a SAML token request. A Result of TokenResponse will be returned
     * if the invocation is successfully. Otherwise, a Result of Status will be returned.
     *
     * @param tokenRequest token request constructed from the client.yml token section.
     * @return Result of TokenResponse or error Status.
     */
    public static Result<TokenResponse> getTokenFromSamlResult(SAMLBearerRequest tokenRequest) {
        return getTokenResult(tokenRequest, null);
    }

    /**
     * Get an access token from the token service based on a SAML token request. A Result of TokenResponse will be returned
     * if the invocation is successfully. Otherwise, a Result of Status will be returned.
     *
     * @param tokenRequest token request constructed from the client.yml token section.
     * @param envTag environment tag for service lookup.
     * @return Result of TokenResponse or error Status.
     */
    public static Result<TokenResponse> getTokenFromSamlResult(SAMLBearerRequest tokenRequest, String envTag) {
        if(tokenClient == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(tokenRequest.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(tokenRequest.getProxyHost(), tokenRequest.getProxyPort() == 0 ? 443 : tokenRequest.getProxyPort())));
                if (tokenRequest.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }

                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                tokenClient = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }
        try {
            String serverUrl = tokenRequest.getServerUrl();
            if(serverUrl == null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                tokenRequest.setServerUrl(cluster.serviceToUrl("https", tokenRequest.getServiceId(), envTag, null));
            }
            if(tokenRequest.getServerUrl() == null) {
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "token"));
            }
            IClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.SAML_BEARER_REQUEST_COMPOSER);
            final HttpRequest request = requestComposer.composeClientRequest(tokenRequest);
            CompletableFuture<HttpResponse<String>> response = tokenClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
            HttpHeaders headers = response.thenApply(HttpResponse::headers).get();
            return handleResponse(getContentTypeHeaders(headers), body);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, tokenRequest.getServerUrl()));
        }
    }

    /**
     * Get the certificate from key distribution service of OAuth 2.0 provider with the kid.
     *
     * @param keyRequest One of the sub classes to get the key for access token or sign token.
     * @return String of the certificate
     * @throws ClientException throw exception if communication with the service fails.
     */
    public static String getKey(KeyRequest keyRequest) throws ClientException {
        if(logger.isDebugEnabled()) logger.debug("keyRequest = " + keyRequest.toString());
        return getKey(keyRequest, null);
    }

    /**
     * Get the token info from the introspection endpoint of OAuth 2.0 provider with the swt.
     * @param token The simple web token that needs to be introspected.
     * @param introspectionRequest One of the subclasses to get the token info.
     * @return String of the token info in JSON
     * @throws ClientException throw exception if communication with the service fails.
     */
    public static Result<String> getIntrospection(String token, IntrospectionRequest introspectionRequest) throws ClientException {
        if(logger.isTraceEnabled()) logger.debug("introspectionRequest = " + introspectionRequest.toString());
        return getIntrospection(token, introspectionRequest, null);
    }

    /**
     * Get the certificate from key distribution service of OAuth 2.0 provider with the kid.
     *
     * @param keyRequest One of the sub classes to get the key for access token or sign token.
     * @param envTag the environment tag from the server.yml for the cluster lookup.
     * @return String of the certificate
     * @throws ClientException throw exception if communication with the service fails.
     */
    public static String getKey(KeyRequest keyRequest, String envTag) throws ClientException {
        String serverUrl = keyRequest.getServerUrl();
        if(serverUrl == null) {
            Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
            serverUrl = cluster.serviceToUrl("https", keyRequest.getServiceId(), envTag, null);
        }
        if(serverUrl == null) {
            throw new ClientException(new Status(OAUTH_SERVER_URL_ERROR, "key"));
        }
        try {
            // The key client is used only during the server startup or jwt key is rotated. Don't cache the keyClient.
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                    .sslContext(HttpClientRequest.createSSLContext());
            if(!StringUtils.isBlank(keyRequest.getProxyHost())) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(keyRequest.getProxyHost(), keyRequest.getProxyPort() == 0 ? 443 : keyRequest.getProxyPort())));
            if (keyRequest.isEnableHttp2()) {
                clientBuilder.version(HttpClient.Version.HTTP_2);
            } else {
                clientBuilder.version(HttpClient.Version.HTTP_1_1);
            }

            // this a workaround to bypass the hostname verification in jdk11 http client.
            Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
            if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                final Properties props = System.getProperties();
                props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
            }
            HttpClient keyClient = clientBuilder.build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(serverUrl + keyRequest.getUri()));
            if(keyRequest.getClientId() != null && keyRequest.getClientSecret() != null) {
                requestBuilder.setHeader(Headers.AUTHORIZATION_STRING, getBasicAuthHeader(keyRequest.getClientId(), keyRequest.getClientSecret()));
            }
            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response =
                    keyClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            return response.thenApply(HttpResponse::body).get(ClientConfig.get().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new ClientException(e);
        }
    }

    /**
     * Get the introspection of the simple web token from OAuth 2.0 provider with the swt.
     * @param token The simple web token to be introspected.
     * @param introspectionRequest One of the subclasses to get the introspection for access token.
     * @param envTag the environment tag from the server.yml for the cluster lookup.
     * @return String of the token info in JSON
     * @throws ClientException throw exception if communication with the service fails.
     */
    public static Result<String> getIntrospection(String token, IntrospectionRequest introspectionRequest, String envTag) throws ClientException {
        if(introspectionClient == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(logger.isTraceEnabled()) logger.trace("proxyHost = " + introspectionRequest.getProxyHost() + " proxyPort = " + introspectionRequest.getProxyPort());
                if(!StringUtils.isBlank(introspectionRequest.getProxyHost())) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(introspectionRequest.getProxyHost(), introspectionRequest.getProxyPort() == 0 ? 443 : introspectionRequest.getProxyPort())));
                if (introspectionRequest.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                introspectionClient = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }

        String serverUrl = introspectionRequest.getServerUrl();
        if(serverUrl == null) {
            Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
            serverUrl = cluster.serviceToUrl("https", introspectionRequest.getServiceId(), envTag, null);
        }
        if(serverUrl == null) {
            throw new ClientException(new Status(OAUTH_SERVER_URL_ERROR, "key"));
        }
        if(logger.isTraceEnabled()) logger.trace("introspection service url = " + serverUrl);
        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("token", token);

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            if(logger.isTraceEnabled()) logger.trace("form = " + form + " url = " + serverUrl + introspectionRequest.getUri() + " clientId = " + introspectionRequest.getClientId() + " clientSecret = " + introspectionRequest.getClientSecret());
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .uri(URI.create(serverUrl + introspectionRequest.getUri()));
            if(introspectionRequest.getClientId() != null && introspectionRequest.getClientSecret() != null) {
                requestBuilder.setHeader(Headers.AUTHORIZATION_STRING, getBasicAuthHeader(introspectionRequest.getClientId(), introspectionRequest.getClientSecret()));
            }
            requestBuilder.setHeader(Headers.CONTENT_TYPE_STRING, APPLICATION_FORM_URLENCODED_VALUE);

            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response =
                    introspectionClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
            if(logger.isTraceEnabled()) logger.trace("body = " + body);
            return Success.of(body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new ClientException(e);
        }
    }

    /**
     * De-reference a simple web token to JWT token from OAuth 2.0 provider. This is normally called from the light-router.
     *
     * @param derefRequest a DerefRequest object that is constructed from the client.yml file.
     * @return String of JWT token
     * @throws ClientException when error occurs.
     */
    public static String derefToken(DerefRequest derefRequest) throws ClientException {
        return derefToken(derefRequest, null);
    }

    /**
     * De-reference a simple web token to JWT token from OAuth 2.0 provider. This is normally called from the light-router.
     *
     * @param derefRequest a DerefRequest object that is constructed from the client.yml file.
     * @param envTag an environment tag from the server.yml for cluster service lookup.
     * @return String of JWT token or a status json if there is an error.
     * @throws ClientException when error occurs.
     */
    public static String derefToken(DerefRequest derefRequest, String envTag) throws ClientException {
        if(derefClient == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(derefRequest.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(derefRequest.getProxyHost(), derefRequest.getProxyPort() == 0 ? 443 : derefRequest.getProxyPort())));
                if (derefRequest.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = ClientConfig.get().getTlsConfig();
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                derefClient = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                throw new ClientException(e);
            }
        }
        try {
            String serverUrl = derefRequest.getServerUrl();
            if(serverUrl == null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                serverUrl = cluster.serviceToUrl("https", derefRequest.getServiceId(), envTag, null);
            }
            if(serverUrl == null) {
                throw new ClientException(new Status(OAUTH_SERVER_URL_ERROR, "deref"));
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(serverUrl + derefRequest.getUri()));
            if(derefRequest.getClientId() != null && derefRequest.getClientSecret() != null) {
                requestBuilder.setHeader(Headers.AUTHORIZATION_STRING, getBasicAuthHeader(derefRequest.getClientId(), derefRequest.getClientSecret()));
            }
            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response = derefClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            return response.thenApply(HttpResponse::body).get(ClientConfig.get().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Exception:", e);
            throw new ClientException(e);
        }
    }

    public static String getBasicAuthHeader(String clientId, String clientSecret) {
        return BASIC + " " + encodeCredentials(clientId, clientSecret);
    }

    public static String encodeCredentials(String clientId, String clientSecret) {
        String cred;
        if(clientSecret != null) {
            cred = clientId + ":" + clientSecret;
        } else {
            cred = clientId;
        }
        String encodedValue;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes(UTF_8));
        encodedValue = new String(encodedBytes, UTF_8);
        return encodedValue;
    }

    public static String getEncodedString(TokenRequest request) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        params.put(GRANT_TYPE, request.getGrantType());
        if(ClientConfig.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            params.put(CODE, ((AuthorizationCodeRequest)request).getAuthCode());
            // The redirectUri can be null so that OAuth 2.0 provider will use the redirectUri defined in the client registration
            if(((AuthorizationCodeRequest)request).getRedirectUri() != null) {
                params.put(ClientConfig.REDIRECT_URI, ((AuthorizationCodeRequest)request).getRedirectUri());
            }
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(ClientConfig.CSRF, csrf);
            }
        }
        if(ClientConfig.CLIENT_AUTHENTICATED_USER.equals(request.getGrantType())) {
            params.put(USER_TYPE, ((ClientAuthenticatedUserRequest)request).getUserType());
            params.put(USER_ID, ((ClientAuthenticatedUserRequest)request).getUserId());
            params.put(ROLES, ((ClientAuthenticatedUserRequest)request).getRoles());
            // The redirectUri can be null so that OAuth 2.0 provider will use the redirectUri defined in the client registration
            if(((ClientAuthenticatedUserRequest)request).getRedirectUri() != null) {
                params.put(ClientConfig.REDIRECT_URI, ((ClientAuthenticatedUserRequest)request).getRedirectUri());
            }
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(ClientConfig.CSRF, csrf);
            }
        }
        if(ClientConfig.REFRESH_TOKEN.equals(request.getGrantType())) {
            params.put(ClientConfig.REFRESH_TOKEN, ((RefreshTokenRequest)request).getRefreshToken());
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(ClientConfig.CSRF, csrf);
            }
        }
        if(request.getScope() != null) {
            params.put(ClientConfig.SCOPE, String.join(" ", request.getScope()));
        }
        if(logger.isTraceEnabled()) logger.trace("token request form data = " + JsonMapper.toJson(params));
        return ClientRequestComposerProvider.getFormDataString(params);
    }

    private static Result<TokenResponse> handleResponse(ContentType contentType, String responseBody) {
        TokenResponse tokenResponse;
        Result<TokenResponse> result;
        if(logger.isTraceEnabled()) logger.trace("contentType = " + contentType + " responseBody = " +  responseBody);
        try {
            //only accept json format response so that can map to a TokenResponse, otherwise escapes server's response and return to the client.
            if(!contentType.equals(ContentType.APPLICATION_JSON)) {
                return Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(contentType, responseBody)));
            }
            if (responseBody != null && responseBody.length() > 0) {
                tokenResponse = Config.getInstance().getMapper().readValue(responseBody, TokenResponse.class);
                // sometimes, the token response contains an error status instead of the access token.
                if(tokenResponse != null && tokenResponse.getAccessToken() != null) {
                    result = Success.of(tokenResponse);
                } else {
                    result = Failure.of(new Status(tokenResponse.getStatusCode(), tokenResponse.getCode(), tokenResponse.getMessage(), tokenResponse.getDescription(), tokenResponse.getSeverity()));
                }
            } else {
                result = Failure.of(new Status(GET_TOKEN_ERROR, "no auth server response"));
                logger.error("Error in token retrieval, response = " + responseBody);
            }
        } catch (UnrecognizedPropertyException e) {
            //in this case, cannot parse success token, which means the server doesn't response a successful token but some messages, we need to pass this message out.
            result = Failure.of(new Status(GET_TOKEN_ERROR, escapeBasedOnType(contentType, responseBody)));
            logger.error("Error in token parsing", e);
        } catch (IOException | RuntimeException e) {
            result = Failure.of(new Status(GET_TOKEN_ERROR, e.getMessage()));
            logger.error("Error in token retrieval", e);
        }
        return result;
    }

    /**
     * populate/renew jwt info to the give jwt object.
     * based on the expire time of the jwt, to determine if need to renew jwt or not.
     * to avoid modifying class member which will case thread-safe problem, move this method from Http2Client to this helper class.
     * @param jwt the given jwt needs to renew or populate
     * @return When success return Jwt; When fail return Status.
     */
    public static Result<Jwt> populateCCToken(final Jwt jwt) {
        boolean isInRenewWindow = jwt.getExpire() - System.currentTimeMillis() < Jwt.getTokenRenewBeforeExpired();
        logger.trace("isInRenewWindow = " + isInRenewWindow);
        //if not in renew window, return the current jwt.
        if(!isInRenewWindow) { return Success.of(jwt); }
        //the same jwt shouldn't be renew at the same time. different jwt shouldn't affect each other's renew activity.
        synchronized (jwt) {
            //if token expired, try to renew synchronously
            if(jwt.getExpire() <= System.currentTimeMillis()) {
                Result<Jwt> result = renewCCTokenSync(jwt);
                if(logger.isTraceEnabled()) logger.trace("Check secondary token is done!");
                return result;
            } else {
                //otherwise renew token silently
                renewCCTokenAsync(jwt);
                if(logger.isTraceEnabled()) logger.trace("Check secondary token is done!");
                return Success.of(jwt);
            }
        }
    }

    /**
     * renew Client Credential token synchronously.
     * When success will renew the Jwt jwt passed in.
     * When fail will return Status code so that can be handled by caller.
     * @param jwt the jwt you want to renew
     * @return Jwt when success, it will be the same object as the jwt you passed in; return Status when fail;
     */
    private static Result<Jwt> renewCCTokenSync(final Jwt jwt) {
        // Already expired, try to renew getCCTokenSynchronously but let requests use the old token.
        logger.trace("In renew window and token is already expired.");
        //the token can be renewed when it's not on renewing or current time is lager than retrying interval
        if (!jwt.isRenewing() || System.currentTimeMillis() > jwt.getExpiredRetryTimeout()) {
            jwt.setRenewing(true);
            jwt.setEarlyRetryTimeout(System.currentTimeMillis() + Jwt.getExpiredRefreshRetryDelay());
            Result<Jwt> result = getCCTokenRemotely(jwt);
            //set renewing flag to false no mater fail or success
            jwt.setRenewing(false);
            return result;
        } else {
            if(logger.isTraceEnabled()) logger.trace("Circuit breaker is tripped and not timeout yet!");
            // token is renewing
            return Failure.of(new Status(STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE));
        }
    }

    /**
     * renew the given Jwt jwt asynchronously.
     * When fail, it will swallow the exception, so no need return type to be handled by caller.
     * @param jwt the jwt you want to renew
     */
    private static void renewCCTokenAsync(final Jwt jwt) {
        // Not expired yet, try to renew async but let requests use the old token.
        logger.trace("In renew window but token is not expired yet.");
        if(!jwt.isRenewing() || System.currentTimeMillis() > jwt.getEarlyRetryTimeout()) {
            jwt.setRenewing(true);
            jwt.setEarlyRetryTimeout(System.currentTimeMillis() + Jwt.getEarlyRefreshRetryDelay());
            logger.trace("Retrieve token async is called while token is not expired yet");

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            executor.schedule(() -> {
                Result<Jwt> result = getCCTokenRemotely(jwt);
                if(result.isFailure()) {
                    // swallow the exception here as it is on a best effort basis.
                    logger.error("Async retrieve token error with status: {}", result.getError().toString());
                }
                //set renewing flag to false after response, doesn't matter if it's success or fail.
                jwt.setRenewing(false);
            }, 50, TimeUnit.MILLISECONDS);
            executor.shutdown();
        }
    }

    /**
     * get Client Credential token from auth server
     * @param jwt the jwt you want to renew
     * @return Jwt when success, it will be the same object as the jwt you passed in; return Status when fail;
     */
    private static Result<Jwt> getCCTokenRemotely(final Jwt jwt) {
        TokenRequest tokenRequest = new ClientCredentialsRequest(jwt.getCcConfig());
        //scopes at this point is may not be set yet when issuing a new token.
        setScope(tokenRequest, jwt);
        if(logger.isTraceEnabled()) logger.trace("TokenRequest = " + JsonMapper.toJson(tokenRequest));
        Result<TokenResponse> result = OauthHelper.getTokenResult(tokenRequest);
        if(result.isSuccess()) {
            TokenResponse tokenResponse = result.getResult();
            jwt.setJwt(tokenResponse.getAccessToken());
            // the expiresIn is seconds and it is converted to millisecond in the future.
            jwt.setExpire(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
            logger.info("Get client credentials token {} with expire_in {} seconds", jwt.getJwt().substring(0, 20), tokenResponse.getExpiresIn());
            //set the scope for future usage.
            jwt.setScopes(tokenResponse.getScope());
            return Success.of(jwt);
        } else {
            logger.info("Get client credentials token fail with status: {}", result.getError().toString());
            return Failure.of(result.getError());
        }
    }

    /**
     * if scopes in jwt.getKey() has value, use this scope
     * otherwise remains the default scope value which already inside tokenRequest when create ClientCredentialsRequest;
     * @param tokenRequest
     * @param jwt
     */
    private static void setScope(TokenRequest tokenRequest, Jwt jwt) {
        if(jwt.getKey() != null && jwt.getKey().getScopes() != null && !jwt.getKey().getScopes().isEmpty()) {
            tokenRequest.setScope(new ArrayList<>() {{ addAll(jwt.getKey().getScopes()); }});
        }
    }

    public static ContentType getContentTypeHeaders(HttpHeaders headers) {
        Optional<String> contentType = headers.firstValue(Headers.CONTENT_TYPE_STRING);
        return contentType.isEmpty() ? ContentType.ANY_TYPE : ContentType.toContentType(contentType.get());
    }

    private static String escapeBasedOnType(ContentType contentType, String responseBody) {
        switch (contentType) {
            case APPLICATION_JSON:
                try {
                    String escapedStr = Config.getInstance().getMapper().writeValueAsString(responseBody);
                    return escapedStr.substring(1,escapedStr.length()-1);
                } catch (JsonProcessingException e) {
                    logger.error("escape json response fails");
                    return responseBody;
                }
            case XML:
                //very rare case because the server should response a json format response
                return escapeXml(responseBody);
            default:
                return responseBody;
        }
    }

    /**
     * Instead of including a large library just for escaping xml, using this util.
     * it should be used in very rare cases because the server should not return xml format message
     * @param nonEscapedXmlStr
     */
    private static String escapeXml (String nonEscapedXmlStr) {
        StringBuilder escapedXML = new StringBuilder();
        for (int i = 0; i < nonEscapedXmlStr.length(); i++) {
            char c = nonEscapedXmlStr.charAt(i);
            switch (c) {
                case '<':
                    escapedXML.append("&lt;");
                    break;
                case '>':
                    escapedXML.append("&gt;");
                    break;
                case '\"':
                    escapedXML.append("&quot;");
                    break;
                case '&':
                    escapedXML.append("&amp;");
                    break;
                case '\'':
                    escapedXML.append("&apos;");
                    break;
                default:
                    if (c > 0x7e) {
                        escapedXML.append("&#" + ((int) c) + ";");
                    } else {
                        escapedXML.append(c);
                    }
            }
        }
        return escapedXML.toString();
    }

}
