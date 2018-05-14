/*
 * Copyright (c) 2018 Network New Technologies Inc.
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

package com.networknt.auth;

import com.networknt.client.oauth.AuthorizationCodeRequest;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.TokenRequest;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;

/**
 * This is a handler that receives authorization code from OAuth 2.0 provider with authorizaiton grant type.
 * It will take the redirected code and get JWT token along with client_id and client_secret defined in
 * client.yml and secret.yml config files. Once the token is received, a session will be created to put the
 * token into.
 *
 * @author Steve Hu
 */
public class StatelessAuthHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatelessAuthHandler.class);
    private static final String CONFIG_NAME = "statelessAuth";
    private static final String CODE = "code";
    private static final String AUTHORIZATION_CODE_MISSING = "ERR10035";

    public static StatelessAuthConfig config =
            (StatelessAuthConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, StatelessAuthConfig.class);

    public StatelessAuthHandler() {
        logger.info("StatelessAuthHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isInfoEnabled()) logger.info("StatelessAuthHandler.handleRequest is called.");
        Deque<String> deque = exchange.getQueryParameters().get(CODE);
        String code = deque == null ? null : deque.getFirst();
        if(logger.isDebugEnabled()) logger.debug("code = " + code);
        // check if code is in the query parameter
        if(code == null || code.trim().length() == 0) {
            Status status = new Status(AUTHORIZATION_CODE_MISSING);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            logger.error("ValidationError:" + status.toString());
            return;
        }
        // use the code and client_id, client_secret to get an access token in jwt format
        String csrf = Util.getUUID();
        TokenRequest request = new AuthorizationCodeRequest();
        ((AuthorizationCodeRequest) request).setAuthCode(code);
        ((AuthorizationCodeRequest) request).setCsrf(csrf);
        TokenResponse response = OauthHelper.getToken(request, config.enableHttp2);
        String accessToken = response.getAccessToken();
        String refreshToken = response.getRefreshToken();
        long expiresIn = response.getExpiresIn();
        String state = response.getState();
        if(logger.isDebugEnabled()) logger.debug("accessToken = " + accessToken + " refreshToken = " + refreshToken + " expiresIn = " + expiresIn + " state = " + state);
        // put all the info into a cookie object
        exchange.setResponseCookie(new CookieImpl("accessToken", accessToken)
                .setDomain(config.cookieDomain)
                .setPath(config.getCookiePath())
                .setMaxAge(config.cookieMaxAge)
                .setHttpOnly(true)
                .setSecure(true));
        exchange.setResponseCookie(new CookieImpl("refreshToken", refreshToken)
                .setDomain(config.cookieDomain)
                .setPath(config.getCookiePath())
                .setMaxAge(config.cookieMaxAge)
                .setHttpOnly(true)
                .setSecure(true));
        exchange.setResponseCookie(new CookieImpl("expiresIn", "" + expiresIn)
                .setDomain(config.cookieDomain)
                .setPath(config.cookiePath)
                .setMaxAge(config.cookieMaxAge)
                .setHttpOnly(true)
                .setSecure(true));
        // this is another csrf token in header and it is accessible for Javascript.
        exchange.getResponseHeaders().put(Constants.CSRF_TOKEN, csrf);
        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.getResponseHeaders().put(Headers.LOCATION, config.getRedirectUri());
        exchange.endExchange();
    }
}
