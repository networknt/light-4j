package com.networknt.client.oauth;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
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

    static final Logger logger = LoggerFactory.getLogger(OauthHelper.class);

    public static Result<TokenResponse> getToken(TokenRequest tokenRequest) {
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
                                        reference.set(handleResponse(string));
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

    public static Result<TokenResponse> getTokenFromSaml(SAMLBearerRequest tokenRequest) {
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
                                        reference.set(handleResponse(string));
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

    private static Result<TokenResponse> handleResponse(String responseBody) {
        TokenResponse tokenResponse;
        Result<TokenResponse> result;
        try {
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
            result = Failure.of(new Status(GET_TOKEN_ERROR, responseBody));
        } catch (IOException | RuntimeException e) {
            result = Failure.of(new Status(GET_TOKEN_ERROR, e.getMessage()));
            logger.error("Error in token retrieval", e);
        }
        return result;
    }

    public static void sendStatusToResponse(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\"));
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
    }
}
