package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.client.oauth.TokenRequest.REDIRECT_URI;
import static com.networknt.client.oauth.TokenRequest.SCOPE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class OauthHelper {
    static final String BASIC = "Basic";
    static final String GRANT_TYPE = "grant_type";
    static final String CODE = "code";

    static final Logger logger = LoggerFactory.getLogger(OauthHelper.class);

    public static TokenResponse getToken(TokenRequest tokenRequest, boolean http2) throws ClientException {
        final AtomicReference<TokenResponse> reference = new AtomicReference<>();
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(tokenRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, http2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }

        try {
            String requestBody = getEncodedString(tokenRequest);
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
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
                                    new StringReadChannelListener(Http2Client.POOL) {

                                        @Override
                                        protected void stringDone(String string) {
                                            logger.debug("getToken response = " + string);
                                            reference.set(handleResponse(string));
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
        } catch (Exception e) {
            logger.error("IOException: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get();
    }

    public static String getKey(KeyRequest keyRequest, boolean http2) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(keyRequest.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, http2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(keyRequest.getUri()).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, getBasicAuthHeader(keyRequest.getClientId(), keyRequest.getClientSecret()));
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
        }
        if(request.getScope() != null) {
            params.put(SCOPE, StringUtils.join(request.getScope(), " "));
        }
        return Http2Client.getFormDataString(params);
    }

    private static TokenResponse handleResponse(String responseBody) {
        TokenResponse tokenResponse = null;
        try {
            if (responseBody != null && responseBody.length() > 0) {
                tokenResponse = Config.getInstance().getMapper().readValue(responseBody, TokenResponse.class);
            } else {
                logger.error("Error in token retrieval, response = " +
                        Encode.forJava(responseBody));
            }
        } catch (IOException | RuntimeException e) {
            logger.error("Error in token retrieval", e);
        }
        return tokenResponse;
    }

}
