package com.networknt.security;

import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.regex.Pattern;

/**
 * Created by steve on 01/09/16.
 */
public class JwtVerifyHandler implements HttpHandler {
    static final XLogger logger = XLoggerFactory.getXLogger(JwtVerifyHandler.class);

    private volatile HttpHandler next;

    public JwtVerifyHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
        String jwt = JwtHelper.getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtClaims claims = JwtHelper.verifyJwt(jwt);
                // put claims into request header so that scope can be verified per endpoint.
                headerMap.add(new HttpString(Constants.CLIENT_ID), claims.getStringClaimValue(Constants.CLIENT_ID));
                headerMap.add(new HttpString(Constants.USER_ID), claims.getStringClaimValue(Constants.USER_ID));
                headerMap.add(new HttpString(Constants.SCOPE), claims.getStringListClaimValue(Constants.SCOPE).toString());
                next.handleRequest(exchange);
            } catch (Exception e) {
                // only log it and unauthorized is returned.
                logger.error("Exception: ", e);
                exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
                exchange.endExchange();
            }
        } else {
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.endExchange();
        }
    }

    public HttpHandler getNext() {
        return next;
    }

    public JwtVerifyHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }


}
