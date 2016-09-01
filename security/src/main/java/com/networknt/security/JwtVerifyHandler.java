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

import java.util.regex.Pattern;

/**
 * Created by steve on 01/09/16.
 */
public class JwtVerifyHandler implements HttpHandler {
    private volatile HttpHandler next = ResponseCodeHandler.HANDLE_404;

    public JwtVerifyHandler(final HttpHandler next) {
        this.next = next;
    }

    public JwtVerifyHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
        if(authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    JwtClaims claims = JwtHelper.verifyJwt(credentials);
                    // put claims into request header so that scope can be verified per endpoint.
                    headerMap.add(new HttpString(Constants.CLIENT_ID), claims.getStringClaimValue(Constants.CLIENT_ID));
                    headerMap.add(new HttpString(Constants.USER_ID), claims.getStringClaimValue(Constants.USER_ID));
                    headerMap.add(new HttpString(Constants.SCOPE), claims.getStringListClaimValue(Constants.SCOPE).toString());
                }
            }
        }

        exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
        exchange.endExchange();
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
