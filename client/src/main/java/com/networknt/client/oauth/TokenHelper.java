package com.networknt.client.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.utility.ApiException;
import com.networknt.utility.ClientException;
import com.networknt.utility.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 02/09/16.
 */
public class TokenHelper {

    static final String BASIC = "Basic";
    static final String GRANT_TYPE = "grant_type";
    static final String CODE = "code";

    static final int HTTP_OK = 200;

    static final Logger logger = LoggerFactory.getLogger(TokenHelper.class);

    public static TokenResponse getToken(TokenRequest tokenRequest) throws ClientException {
        String url = tokenRequest.getServerUrl() + tokenRequest.getUri();
        TokenResponse tokenResponse = null;

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader(Constants.AUTHORIZATION,
                getBasicAuthHeader(tokenRequest.getClientId(), tokenRequest.getClientSecret()));

        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            //httpPost.setEntity(getEntity(tokenRequest));
            HttpResponse response = client.execute(httpPost);
            tokenResponse = handleResponse(response);

        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: ", jpe);
            throw new ClientException("JsonProcessingException: ", jpe);
        } catch (UnsupportedEncodingException uee) {
            logger.error("UnsupportedEncodingException", uee);
            throw new ClientException("UnsupportedEncodingException: ", uee);
        } catch (IOException ioe) {
            logger.error("IOException: ", ioe);
            throw new ClientException("IOException: ", ioe);
        }
        return tokenResponse;
    }

    public static String getBasicAuthHeader(String clientId, String clientSecret) {
        return BASIC + " " + encodeCredentials(clientId, clientSecret);
    }

    public static String encodeCredentials(String clientId, String clientSecret) {
        String cred = "";
        if(clientSecret != null) {
            cred = clientId + ":" + clientSecret;
        } else {
            cred = clientId;
        }
        String encodedValue = null;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        encodedValue = new String(encodedBytes);
        return encodedValue;
    }

    public static String decodeCredentials(String cred) {
        return new String(Base64.decodeBase64(cred));
    }

    private static StringEntity getEntity(TokenRequest request) throws JsonProcessingException, UnsupportedEncodingException {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put(GRANT_TYPE, request.getGrantType());
        if(TokenRequest.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            map.put(CODE, ((AuthorizationCodeRequest)request).getAuthCode());
            map.put(TokenRequest.REDIRECT_URI, ((AuthorizationCodeRequest)request).getRedirectUri());
        }
        if(request.getScope() != null) {
            map.put(TokenRequest.SCOPE, StringUtils.join(request.getScope(), " "));
        }
        return new StringEntity(Config.getInstance().getMapper().writeValueAsString(map));
    }

    private static TokenResponse handleResponse(HttpResponse response) throws ClientException {
        TokenResponse tokenResponse = null;
        int statusCode = response.getStatusLine().getStatusCode();
        try {
            if (statusCode == HTTP_OK) {
                tokenResponse = (TokenResponse) Config.getInstance().getMapper()
                        .readValue(response.getEntity().getContent(),
                                TokenResponse.class);
            } else {
                logger.error("Error in token retrieval, response = " +
                        Encode.forJava(EntityUtils.toString(response.getEntity())));
                throw new ClientException("Error in token retrieval, status code = " + statusCode);
            }
        } catch (ParseException e) {
            logger.error("Error in token retrieval", e);
            throw new ClientException("Error in token retrieval", e);
        } catch (IOException e) {
            logger.error("Error in token retrieval", e);
            throw new ClientException("Error in token retrieval", e);
        } catch (RuntimeException e) {
            logger.error("Error in token retrieval", e);
            throw new ClientException("Error in token retrieval", e);
        }
        return tokenResponse;
    }

}
