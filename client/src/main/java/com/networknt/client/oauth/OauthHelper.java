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

package com.networknt.client.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.httpstring.ContentType;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
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
    static final String BASIC = "Basic";
    static final String GRANT_TYPE = "grant_type";
    static final String CODE = "code";
    private static final String FAIL_TO_SEND_REQUEST = "ERR10051";
    private static final String GET_TOKEN_ERROR = "ERR10052";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String GET_TOKEN_TIMEOUT = "ERR10054";
    public static final String STATUS_CLIENT_CREDENTIALS_TOKEN_NOT_AVAILABLE = "ERR10009";

    static final Logger logger = LoggerFactory.getLogger(OauthHelper.class);

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

    public static Result<TokenResponse> getTokenResult(TokenRequest tokenRequest) {
        final AtomicReference<Result<TokenResponse>> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(tokenRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, tokenRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            logger.error("cannot establish connection: {}", e.getStackTrace());
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, tokenRequest.getServerUrl()));
        }

        try {
            String requestBody = getEncodedString(tokenRequest);
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                request.getRequestHeaders().put(Headers.AUTHORIZATION, getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()));
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
            connection = client.connect(new URI(tokenRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, tokenRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            logger.error("cannot establish connection: {}", e.getStackTrace());
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR));
        }

        try {
            Map<String, String> postBody = new HashMap<String, String>();
            postBody.put(SAMLBearerRequest.GRANT_TYPE_KEY , SAMLBearerRequest.GRANT_TYPE_VALUE );
            postBody.put(SAMLBearerRequest.ASSERTION_KEY, tokenRequest.getSamlAssertion());
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_TYPE_KEY, SAMLBearerRequest.CLIENT_ASSERTION_TYPE_VALUE);
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_KEY, tokenRequest.getJwtClientAssertion());
            String requestBody = Http2Client.getFormDataString(postBody);
            logger.debug(requestBody);

            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");

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

    public static String getKey(KeyRequest keyRequest) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(keyRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, keyRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
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

    public static String derefToken(DerefRequest derefRequest) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(derefRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, derefRequest.enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
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

    private static String getEncodedString(TokenRequest request) throws UnsupportedEncodingException {
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
            params.put(SCOPE, String.join(" ", request.getScope()));
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
                if(tokenResponse != null) {
                    result = Success.of(tokenResponse);
                } else {
                    result = Failure.of(new Status(GET_TOKEN_ERROR, responseBody));
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
    public static Result<Jwt> populateCCToken(Jwt jwt) {
        boolean isInRenewWindow = jwt.getExpire() - System.currentTimeMillis() < jwt.getTokenRenewBeforeExpired();
        logger.trace("isInRenewWindow = " + isInRenewWindow);
        //if not in renew window, return the current jwt.
        if(!isInRenewWindow) { return Success.of(jwt); }
        //block other getting token requests, only once at a time.
        //Once one request get the token, other requests don't need to get from auth server anymore.
        synchronized (OauthHelper.class) {
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
            jwt.setEarlyRetryTimeout(System.currentTimeMillis() + jwt.getExpiredRefreshRetryDelay());
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
    private static void renewCCTokenAsync(Jwt jwt) {
        // Not expired yet, try to renew async but let requests use the old token.
        logger.trace("In renew window but token is not expired yet.");
        if(!jwt.isRenewing() || System.currentTimeMillis() > jwt.getEarlyRetryTimeout()) {
            jwt.setRenewing(true);
            jwt.setEarlyRetryTimeout(System.currentTimeMillis() + jwt.getEarlyRefreshRetryDelay());
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
        Result<TokenResponse> result = OauthHelper.getTokenResult(tokenRequest);
        if(result.isSuccess()) {
            TokenResponse tokenResponse = result.getResult();
            jwt.setJwt(tokenResponse.getAccessToken());
            // the expiresIn is seconds and it is converted to millisecond in the future.
            jwt.setExpire(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
            logger.info("Get client credentials token {} with expire_in {} seconds", jwt, tokenResponse.getExpiresIn());
            return Success.of(jwt);
        } else {
            logger.info("Get client credentials token fail with status: {}", result.getError().toString());
            return Failure.of(result.getError());
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
}
