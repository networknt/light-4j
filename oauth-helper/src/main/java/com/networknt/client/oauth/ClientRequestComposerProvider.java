package com.networknt.client.oauth;

import com.networknt.http.client.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

import static com.networknt.client.oauth.ClientRequestComposerProvider.ClientRequestComposers.*;

/**
 * This class is a singleton to provide registered IClientRequestComposable composers.
 * The composer is to compose requests to get {ClientCredential token, SAML token}.
 * This provider can be extended to support other type tokens.
 * If not register any IClientRequestComposable composer, it will init default composers(DefaultClientCredentialRequestComposer, DefaultSAMLBearerRequestComposer).
 * To see composer please check {@link com.networknt.client.oauth.IClientRequestComposable}
 */
public class ClientRequestComposerProvider {
    public enum ClientRequestComposers { CLIENT_CREDENTIAL_REQUEST_COMPOSER, SAML_BEARER_REQUEST_COMPOSER, CLIENT_AUTHENTICATED_USER_REQUEST_COMPOSER }
    private static final ClientRequestComposerProvider INSTANCE = new ClientRequestComposerProvider();
    private Map<ClientRequestComposers, IClientRequestComposable> composersMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ClientRequestComposerProvider.class);
    private ClientRequestComposerProvider() {
    }

    public static ClientRequestComposerProvider getInstance() {
        return INSTANCE;
    }

    /**
     * get IClientRequestComposable based on ClientRequestComposers composer name
     * @param composerName composer name
     * @return IClientRequestComposable composer
     */
    public IClientRequestComposable getComposer(ClientRequestComposers composerName) {
        IClientRequestComposable composer = composersMap.get(composerName);
        if(composer == null) {
            initDefaultComposer(composerName);
        }
        return composersMap.get(composerName);
    }

    private void initDefaultComposer(ClientRequestComposers composerName) {
        switch (composerName) {
            case CLIENT_CREDENTIAL_REQUEST_COMPOSER:
                composersMap.put(CLIENT_CREDENTIAL_REQUEST_COMPOSER, new DefaultClientCredentialRequestComposer());
                break;
            case SAML_BEARER_REQUEST_COMPOSER:
                composersMap.put(SAML_BEARER_REQUEST_COMPOSER, new DefaultSAMLBearerRequestComposer());
                break;
            case CLIENT_AUTHENTICATED_USER_REQUEST_COMPOSER:
                composersMap.put(CLIENT_AUTHENTICATED_USER_REQUEST_COMPOSER, new DefaultClientAuthenticatedUserRequestComposer());
                break;
            default:
                break;
        }
    }

    /**
     * register the composer in this provider with Enum ClientRequestComposers name.
     * after registration, you will get what you've registered with the same Enum ClientRequestComposers name.
     * @param composerName  ClientRequestComposers composer name
     * @param composer IClientRequestComposable composer
     */
    public void registerComposer(ClientRequestComposers composerName, IClientRequestComposable composer) {
        composersMap.put(composerName, composer);
    }

    /**
     * the default composer to compose a ClientRequest with the given TokenRequest to get SAML token.
     */
    private static class DefaultSAMLBearerRequestComposer implements IClientRequestComposable {

        @Override
        public HttpRequest composeClientRequest(TokenRequest tokenRequest) {
            final HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(composeRequestBody(tokenRequest)))
                    .uri(URI.create(tokenRequest.getServerUrl() + tokenRequest.getUri()))
                    .header(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded")
                    .build();
            return request;
        }

        public String composeRequestBody(TokenRequest tokenRequest) {
            SAMLBearerRequest SamlTokenRequest = (SAMLBearerRequest)tokenRequest;
            Map<String, String> postBody = new HashMap<>();
            postBody.put(SAMLBearerRequest.GRANT_TYPE_KEY , SAMLBearerRequest.GRANT_TYPE_VALUE );
            postBody.put(SAMLBearerRequest.ASSERTION_KEY, SamlTokenRequest.getSamlAssertion());
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_TYPE_KEY, SAMLBearerRequest.CLIENT_ASSERTION_TYPE_VALUE);
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_KEY, SamlTokenRequest.getJwtClientAssertion());
            try {
                return getFormDataString(postBody);
            } catch (UnsupportedEncodingException e) {
                logger.error("get encoded string from tokenRequest fails: \n {}", e.toString());
            }
            return "";
        }
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

    /**
     * the default composer to compose a ClientRequest with the given TokenRequest to get ClientCredential token.
     */
    private static class DefaultClientCredentialRequestComposer implements IClientRequestComposable {

        @Override
        public HttpRequest composeClientRequest(TokenRequest tokenRequest) {
            final HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(composeRequestBody(tokenRequest)))
                    .uri(URI.create(tokenRequest.getServerUrl() + tokenRequest.getUri()))
                    .setHeader(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded")
                    .setHeader(Headers.ACCEPT_STRING, "application/json")
                    .setHeader(Headers.AUTHORIZATION_STRING, OauthHelper.getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()))
                    .build();
            if(logger.isTraceEnabled()) logger.trace("request = " + request.toString());
            return request;
        }

        public String composeRequestBody(TokenRequest tokenRequest) {
            try {
                return OauthHelper.getEncodedString(tokenRequest);
            } catch (UnsupportedEncodingException e) {
                logger.error("get encoded string from tokenRequest fails: \n {}", e.toString());
            }
            return "";
        }
    }

    /**
     * the default composer to compose a ClientRequest with the given TokenRequest to get ClientAuthenticatedUser token.
     */
    private static class DefaultClientAuthenticatedUserRequestComposer implements IClientRequestComposable {

        @Override
        public HttpRequest composeClientRequest(TokenRequest tokenRequest) {
            final HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(composeRequestBody(tokenRequest)))
                    .uri(URI.create(tokenRequest.getServerUrl() + tokenRequest.getUri()))
                    .setHeader(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded")
                    .setHeader(Headers.AUTHORIZATION_STRING, OauthHelper.getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()))
                    .build();

            return request;
        }

        public String composeRequestBody(TokenRequest tokenRequest) {
            try {
                return OauthHelper.getEncodedString(tokenRequest);
            } catch (UnsupportedEncodingException e) {
                logger.error("get encoded string from tokenRequest fails: \n {}", e.toString());
            }
            return "";
        }
    }
}
