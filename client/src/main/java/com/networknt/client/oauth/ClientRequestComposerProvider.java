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

/**
 * This class is a singleton to provide registered IClientRequestComposable composers.
 * The composer is to compose requests to get {ClientCredential token, SAML token}.
 * This provider can be extended to support other type tokens.
 * If not register any IClientRequestComposable composer, it will init default composers(DefaultClientCredentialRequestComposer, DefaultSAMLBearerRequestComposer).
 * To see composer please check {@link com.networknt.client.oauth.IClientRequestComposable}
 */
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
        public ClientRequest composeClientRequest(TokenRequest tokenRequest) {
            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            return request;
        }

        @Override
        public String composeRequestBody(TokenRequest tokenRequest) {
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

    /**
     * the default composer to compose a ClientRequest with the given TokenRequest to get ClientCredential token.
     */
    private static class DefaultClientCredentialRequestComposer implements IClientRequestComposable {

        @Override
        public ClientRequest composeClientRequest(TokenRequest tokenRequest) {
            final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(tokenRequest.getUri());
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, OauthHelper.getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()));
            return request;
        }

        @Override
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
