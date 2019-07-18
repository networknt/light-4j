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
import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.ContentType;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.client.oauth.TokenRequest.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OauthHelper {
    private static final String BASIC = "Basic";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CODE = "code";

    /**
     * @deprecated will be moved to {@link ClientConfig#SCOPE}
     */
    @Deprecated
    static final String SCOPE = "scope";

    /**
     * @deprecated will be moved to {@link ClientConfig#SERVICE_ID}
     */
    @Deprecated
    static final String SERVICE_ID = "service_id";
    private static final String FAIL_TO_SEND_REQUEST = "ERR10051";
    private static final String GET_TOKEN_ERROR = "ERR10052";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String GET_TOKEN_TIMEOUT = "ERR10054";
    public static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERR10009";

    private static final Logger logger = LoggerFactory.getLogger(OauthHelper.class);

    /**
     * @deprecated As of release 1.5.29, replaced with @link #getTokenResult(TokenRequest tokenRequest)
     * @param tokenRequest Request details for the token
     * @return A TokenResponse on success
     * @throws ClientException If any issues
     */
    @Deprecated
    public static TokenResponse getToken(TokenRequest tokenRequest) throws ClientException {
        Result<TokenResponse> responseResult = getTokenResult(tokenRequest);
        if (responseResult.isSuccess()) {
            return responseResult.getResult();
        }
        throw new ClientException(responseResult.getError());
    }

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
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            if(tokenRequest.getServerUrl() != null) {
                connection = client.connect(new URI(tokenRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, tokenRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else if(tokenRequest.getServiceId() != null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                String url = cluster.serviceToUrl("https", tokenRequest.getServiceId(), envTag, null);
                connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, tokenRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else {
                // both server_url and serviceId are empty in the config.
                logger.error("Error: both server_url and serviceId are not configured in client.yml for " + tokenRequest.getClass());
                throw new ClientException("both server_url and serviceId are not configured in client.yml for " + tokenRequest.getClass());
            }
        } catch (Exception e) {
            logger.error("cannot establish connection:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, tokenRequest.getServerUrl() != null? tokenRequest.getServerUrl() : tokenRequest.getServiceId()));
        }

        try {
            IClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.CLIENT_CREDENTIAL_REQUEST_COMPOSER);

            connection.getIoThread().execute(new TokenRequestAction(tokenRequest, requestComposer, connection, reference, latch));

            latch.await(4, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            return Failure.of(new Status(FAIL_TO_SEND_REQUEST));
        } finally {
            IoUtils.safeClose(connection);
        }

        //if reference.get() is null at this point, mostly likely couldn't get token within latch.await() timeout.
        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_TIMEOUT)) : reference.get();
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
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            if(signRequest.getServerUrl() != null) {
                connection = client.connect(new URI(signRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, signRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else if(signRequest.getServiceId() != null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                String url = cluster.serviceToUrl("https", signRequest.getServiceId(), envTag, null);
                connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, signRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else {
                // both server_url and serviceId are empty in the config.
                logger.error("Error: both server_url and serviceId are not configured in client.yml for " + signRequest.getClass());
                throw new ClientException("both server_url and serviceId are not configured in client.yml for " + signRequest.getClass());
            }
        } catch (Exception e) {
            logger.error("cannot establish connection:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, signRequest.getServerUrl() != null ? signRequest.getServerUrl() : signRequest.getServiceId()));
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("expires", signRequest.getExpires());
            map.put("payload", signRequest.getPayload());
            String requestBody = Config.getInstance().getMapper().writeValueAsString(map);
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(signRequest.getUri());
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                request.getRequestHeaders().put(Headers.AUTHORIZATION, getBasicAuthHeader(signRequest.getClientId(), signRequest.getClientSecret()));
                connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
                        result.setResponseListener(new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringReadChannelListener(Http2Client.BUFFER_POOL) {

                                    @Override
                                    protected void stringDone(String string) {

                                        logger.debug("getToken response = " + string);
                                        reference.set(handleResponse(getContentTypeFromExchange(result), string));
                                        latch.countDown();
                                    }

                                    @Override
                                    protected void error(IOException e) {
                                        logger.error("IOException:", e);
                                        reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                                        latch.countDown();
                                    }
                                }.setup(result.getResponseChannel());
                            }

                            @Override
                            public void failed(IOException e) {
                                logger.error("IOException:", e);
                                reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        logger.error("IOException:", e);
                        reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                        latch.countDown();
                    }
                });
            });

            latch.await(signRequest.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            return Failure.of(new Status(FAIL_TO_SEND_REQUEST));
        } finally {
            IoUtils.safeClose(connection);
        }

        //if reference.get() is null at this point, mostly likely couldn't get token within latch.await() timeout.
        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_TIMEOUT)) : reference.get();
    }

    /**
     * @deprecated As of release 1.5.29, replaced with @link #getTokenFromSamlResult(SAMLBearerRequest tokenRequest)
     *
     * @param tokenRequest Request details for the token
     * @return A TokenResponse object on success
     * @throws ClientException If any issues
     */
    @Deprecated
    public static TokenResponse getTokenFromSaml(SAMLBearerRequest tokenRequest) throws ClientException {
        Result<TokenResponse> responseResult = getTokenFromSamlResult(tokenRequest);
        if (responseResult.isSuccess()) {
            return responseResult.getResult();
        }
        throw new ClientException(responseResult.getError());
    }

    public static Result<TokenResponse> getTokenFromSamlResult(SAMLBearerRequest tokenRequest) {
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(tokenRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, tokenRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            logger.error("cannot establish connection:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR));
        }
        try {
            IClientRequestComposable requestComposer = ClientRequestComposerProvider.getInstance().getComposer(ClientRequestComposerProvider.ClientRequestComposers.SAML_BEARER_REQUEST_COMPOSER);

            connection.getIoThread().execute(new TokenRequestAction(tokenRequest, requestComposer, connection, reference, latch));

            latch.await(4, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("IOException: ", e);
            return Failure.of(new Status(FAIL_TO_SEND_REQUEST));
        } finally {
            IoUtils.safeClose(connection);
        }
        //if reference.get() is null at this point, mostly likely couldn't get token within latch.await() timeout.
        return reference.get() == null ? Failure.of(new Status(GET_TOKEN_TIMEOUT)) : reference.get();
    }

    /**
     * This private class is to encapsulate the action to send request, and handling response,
     * because of the way of sending request to get token and handle exceptions are the same for both JWT and SAML.
     * The only difference is how to compose the request based on TokenRequest model.
     */
    private static class TokenRequestAction implements Runnable{
        private ClientConnection connection;
        private AtomicReference<Result<TokenResponse>> reference;
        private CountDownLatch latch;
        private IClientRequestComposable requestComposer;
        private TokenRequest tokenRequest;

        TokenRequestAction(TokenRequest tokenRequest, IClientRequestComposable requestComposer, ClientConnection connection, AtomicReference<Result<TokenResponse>> reference, CountDownLatch latch){
            this.tokenRequest = tokenRequest;
            this.connection = connection;
            this.reference = reference;
            this.latch = latch;
            this.requestComposer = requestComposer;
        }
        @Override
        public void run() {
            final ClientRequest request = requestComposer.composeClientRequest(tokenRequest);
            String requestBody = requestComposer.composeRequestBody(tokenRequest);
            if (logger.isDebugEnabled()) {
                logger.debug("The request sent to the oauth server = request header(s): {}, request body: {}", request.getRequestHeaders().toString(), requestBody);
            }
            adjustNoChunkedEncoding(request, requestBody);
            connection.sendRequest(request, new ClientCallback<ClientExchange>() {

                @Override
                public void completed(ClientExchange result) {
                    new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
                    result.setResponseListener(new ClientCallback<ClientExchange>() {
                        @Override
                        public void completed(ClientExchange result) {
                            new StringReadChannelListener(Http2Client.BUFFER_POOL) {

                                @Override
                                protected void stringDone(String string) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("getToken response = " + string);
                                    }
                                    reference.set(handleResponse(getContentTypeFromExchange(result), string));
                                    latch.countDown();
                                }

                                @Override
                                protected void error(IOException e) {
                                    logger.error("IOException:", e);
                                    reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                                    latch.countDown();
                                }
                            }.setup(result.getResponseChannel());
                        }

                        @Override
                        public void failed(IOException e) {
                            logger.error("IOException:", e);
                            reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                            latch.countDown();
                        }
                    });
                }

                @Override
                public void failed(IOException e) {
                    logger.error("IOException:", e);
                    reference.set(Failure.of(new Status(FAIL_TO_SEND_REQUEST)));
                    latch.countDown();
                }
            });
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
        return getKey(keyRequest, null);
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
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            if(keyRequest.getServerUrl() != null) {
                connection = client.connect(new URI(keyRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, keyRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else if(keyRequest.getServiceId() != null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                String url = cluster.serviceToUrl("https", keyRequest.getServiceId(), envTag, null);
                connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, keyRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else {
                // both server_url and serviceId are empty in the config.
                logger.error("Error: both server_url and serviceId are not configured in client.yml for " + keyRequest.getClass());
                throw new ClientException("both server_url and serviceId are not configured in client.yml for " + keyRequest.getClass());
            }
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(keyRequest.getUri()).setMethod(Methods.GET);

            if (keyRequest.getClientId()!=null) {
                request.getRequestHeaders().put(Headers.AUTHORIZATION, getBasicAuthHeader(keyRequest.getClientId(), keyRequest.getClientSecret()));
            }
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            adjustNoChunkedEncoding(request, "");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
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
     * @return String of JWT token
     * @throws ClientException when error occurs.
     */
    public static String derefToken(DerefRequest derefRequest, String envTag) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            if(derefRequest.getServerUrl() != null) {
                connection = client.connect(new URI(derefRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, derefRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else if(derefRequest.getServiceId() != null) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                String url = cluster.serviceToUrl("https", derefRequest.getServiceId(), envTag, null);
                connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, derefRequest.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
            } else {
                // both server_url and serviceId are empty in the config.
                logger.error("Error: both server_url and serviceId are not configured in client.yml for " + derefRequest.getClass());
                throw new ClientException("both server_url and serviceId are not configured in client.yml for " + derefRequest.getClass());
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(derefRequest.getUri()).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, getBasicAuthHeader(derefRequest.getClientId(), derefRequest.getClientSecret()));
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
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
        if(TokenRequest.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            params.put(CODE, ((AuthorizationCodeRequest)request).getAuthCode());
            params.put(REDIRECT_URI, ((AuthorizationCodeRequest)request).getRedirectUri());
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(CSRF, csrf);
            }
        }
        if(TokenRequest.REFRESH_TOKEN.equals(request.getGrantType())) {
            params.put(REFRESH_TOKEN, ((RefreshTokenRequest)request).getRefreshToken());
            String csrf = request.getCsrf();
            if(csrf != null) {
                params.put(CSRF, csrf);
            }
        }
        if(request.getScope() != null) {
            params.put(ClientConfig.SCOPE, String.join(" ", request.getScope()));
        }
        return Http2Client.getFormDataString(params);
    }

    private static Result<TokenResponse> handleResponse(ContentType contentType, String responseBody) {
        TokenResponse tokenResponse;
        Result<TokenResponse> result;
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
        } catch (IOException | RuntimeException e) {
            result = Failure.of(new Status(GET_TOKEN_ERROR, e.getMessage()));
            logger.error("Error in token retrieval", e);
        }
        return result;
    }

    /**
     * @deprecated
     * @param exchange each handler should use LightHttpHandler#setExchangeStatus
     * @param status Status
     */
    @Deprecated
    public static void sendStatusToResponse(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
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
        //the token can be renew when it's not on renewing or current time is lager than retrying interval
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
        TokenRequest tokenRequest = new ClientCredentialsRequest();
        //scopes at this point is may not be set yet when issuing a new token.
        setScope(tokenRequest, jwt);
        Result<TokenResponse> result = OauthHelper.getTokenResult(tokenRequest);
        if(result.isSuccess()) {
            TokenResponse tokenResponse = result.getResult();
            jwt.setJwt(tokenResponse.getAccessToken());
            // the expiresIn is seconds and it is converted to millisecond in the future.
            jwt.setExpire(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
            logger.info("Get client credentials token {} with expire_in {} seconds", jwt, tokenResponse.getExpiresIn());
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
        if(jwt.getKey() != null && !jwt.getKey().getScopes().isEmpty()) {
            tokenRequest.setScope(new ArrayList<String>() {{ addAll(jwt.getKey().getScopes()); }});
        }
    }

    public static ContentType getContentTypeFromExchange(ClientExchange exchange) {
        HeaderValues headerValues = exchange.getResponse().getResponseHeaders().get(Headers.CONTENT_TYPE);
        return headerValues == null ? ContentType.ANY_TYPE : ContentType.toContentType(headerValues.getFirst());
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

    /**
     * this method is to support sending a server which doesn't support chunked transfer encoding.
     * @param request ClientRequest
     * @param requestBody String
     */
    public static void adjustNoChunkedEncoding(ClientRequest request, String requestBody) {
        String fixedLengthString = request.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        String transferEncodingString = request.getRequestHeaders().getLast(Headers.TRANSFER_ENCODING);
        if(transferEncodingString != null) {
            request.getRequestHeaders().remove(Headers.TRANSFER_ENCODING);
        }
        //if already specify a content-length, should use what they provided
        if(fixedLengthString != null && Long.parseLong(fixedLengthString) > 0) {
            return;
        }
        if(!StringUtils.isEmpty(requestBody)) {
            long contentLength = requestBody.getBytes(UTF_8).length;
            request.getRequestHeaders().put(Headers.CONTENT_LENGTH, contentLength);
        }

    }
}
