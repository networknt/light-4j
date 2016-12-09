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
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.utility.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            httpPost.setEntity(getEntity(tokenRequest));
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

    private static UrlEncodedFormEntity getEntity(TokenRequest request) throws JsonProcessingException, UnsupportedEncodingException {
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair(GRANT_TYPE, request.getGrantType()));
        if(TokenRequest.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            urlParameters.add(new BasicNameValuePair(CODE, ((AuthorizationCodeRequest)request).getAuthCode()));
            urlParameters.add(new BasicNameValuePair(TokenRequest.REDIRECT_URI, ((AuthorizationCodeRequest)request).getRedirectUri()));
        }
        if(request.getScope() != null) {
            urlParameters.add(new BasicNameValuePair(TokenRequest.SCOPE, StringUtils.join(request.getScope(), " ")));
        }
        return new UrlEncodedFormEntity(urlParameters);
    }

    private static TokenResponse handleResponse(HttpResponse response) throws ClientException {
        TokenResponse tokenResponse = null;
        int statusCode = response.getStatusLine().getStatusCode();
        try {
            if (statusCode == HTTP_OK) {
                tokenResponse = Config.getInstance().getMapper()
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
