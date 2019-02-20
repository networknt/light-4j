package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.networknt.client.oauth.ClientRequestComposerProvider.ClientRequestComposers.CLIENT_CREDENTIAL_REQUEST_COMPOSER;
import static com.networknt.client.oauth.ClientRequestComposerProvider.ClientRequestComposers.SAML_BEARER_REQUEST_COMPOSER;
import static com.networknt.client.oauth.OauthHelper.CODE;
import static com.networknt.client.oauth.OauthHelper.GRANT_TYPE;
import static com.networknt.client.oauth.TokenRequest.*;

public class ClientRequestComposerProvider {
    public enum ClientRequestComposers { CLIENT_CREDENTIAL_REQUEST_COMPOSER, SAML_BEARER_REQUEST_COMPOSER }
    private static final ClientRequestComposerProvider INSTANCE = new ClientRequestComposerProvider();
    private Map<ClientRequestComposers, IClientRequestComposable> composersMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ClientRequestComposerProvider.class);
    private ClientRequestComposerProvider() {
    }

    public static ClientRequestComposerProvider getInstance() {
        return INSTANCE;
    }

    public IClientRequestComposable getComposer(ClientRequestComposers composerName) {
        IClientRequestComposable composer = composersMap.get(composerName);
        if(composer == null) {
            initDefaultComposer(composerName);
        }
        return composer;
    }

    private void initDefaultComposer(ClientRequestComposers composerName) {
        switch (composerName) {
            case CLIENT_CREDENTIAL_REQUEST_COMPOSER:
                composersMap.put(CLIENT_CREDENTIAL_REQUEST_COMPOSER, new DefaultClientCredentialRequestComposer());
                break;
            case SAML_BEARER_REQUEST_COMPOSER:
                composersMap.put(SAML_BEARER_REQUEST_COMPOSER, new DefaultSAMLBearerRequestComposer());
                break;
        }
    }

    public void registerComposer(ClientRequestComposers composerName, IClientRequestComposable composer) {
        composersMap.put(composerName, composer);
    }

    private static class DefaultSAMLBearerRequestComposer implements IClientRequestComposable {

        @Override
        public ClientRequest ComposeClientRequest(TokenRequest tokenRequest) {
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            return request;
        }

        @Override
        public String ComposeRequestBody(TokenRequest tokenRequest) {
            SAMLBearerRequest SamlTokenRequest = (SAMLBearerRequest)tokenRequest;
            Map<String, String> postBody = new HashMap<>();
            postBody.put(SAMLBearerRequest.GRANT_TYPE_KEY , SAMLBearerRequest.GRANT_TYPE_VALUE );
            postBody.put(SAMLBearerRequest.ASSERTION_KEY, SamlTokenRequest.getSamlAssertion());
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_TYPE_KEY, SAMLBearerRequest.CLIENT_ASSERTION_TYPE_VALUE);
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_KEY, SamlTokenRequest.getJwtClientAssertion());
            try {
                return Http2Client.getFormDataString(postBody);
            } catch (UnsupportedEncodingException e) {
                logger.error("get encoded string from tokenRequest fails: \n {}", e.toString());
            }
            return "";
        }
    }

    private static class DefaultClientCredentialRequestComposer implements IClientRequestComposable {

        @Override
        public ClientRequest ComposeClientRequest(TokenRequest tokenRequest) {
            final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, OauthHelper.getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()));
            return request;
        }

        @Override
        public String ComposeRequestBody(TokenRequest tokenRequest) {
            try {
                return getEncodedString(tokenRequest);
            } catch (UnsupportedEncodingException e) {
                logger.error("get encoded string from tokenRequest fails: \n {}", e.toString());
            }
            return "";
        }
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

}
