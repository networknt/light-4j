package com.networknt.security;

import com.networknt.exception.ExpiredTokenException;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractSimpleJwtVerifyHandler extends UndertowVerifyHandler implements MiddlewareHandler, IJwtVerifyHandler {
    static final Logger logger = LoggerFactory.getLogger(AbstractSimpleJwtVerifyHandler.class);
    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String TOKEN_VERIFICATION_EXCEPTION = "ERR10090";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    public static SecurityConfig config;

    // make this static variable public so that it can be accessed from the server-info module
    public static JwtVerifier jwtVerifier;

    public volatile HttpHandler next;

    @Override
    @SuppressWarnings("unchecked")
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        if (logger.isDebugEnabled())
            logger.debug("SimpleJwtVerifyHandler.handleRequest starts.");

        String reqPath = exchange.getRequestPath();

        // if request path is in the skipPathPrefixes in the config, call the next handler directly to skip the security check.
        if (config.getSkipPathPrefixes() != null && config.getSkipPathPrefixes().stream().anyMatch(reqPath::startsWith)) {
            if(logger.isTraceEnabled())
                logger.trace("Skip request path base on skipPathPrefixes for {}", reqPath);
            Handler.next(exchange, next);
            if (logger.isDebugEnabled())
                logger.debug("SimpleJwtVerifyHandler.handleRequest ends.");
            return;
        }
        // only UnifiedSecurityHandler will have the jwkServiceIds as the third parameter.
        Status status = handleJwt(exchange, null, reqPath, null);
        if(status != null) {
            setExchangeStatus(exchange, status);
            exchange.endExchange();
        } else {
            if(logger.isDebugEnabled()) logger.debug("SimpleJwtVerifyHandler.handleRequest ends.");
            Handler.next(exchange, next);
        }
    }

    public Status handleJwt(HttpServerExchange exchange, String pathPrefix, String reqPath, List<String> jwkServiceIds) throws Exception {
        Map<String, Object> auditInfo = null;
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);

        if (logger.isTraceEnabled() && authorization != null && authorization.length() > 10)
            logger.trace("Authorization header = {}", authorization.substring(0, 10));
        // if an empty authorization header or a value length less than 6 ("Basic "), return an error
        if(authorization == null ) {
            Status status = new Status(STATUS_MISSING_AUTH_TOKEN);
            if (logger.isTraceEnabled()) logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
            return status;
        } else if(authorization.trim().length() < 6) {
            Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
            if (logger.isTraceEnabled()) logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
            return status;
        } else {
            authorization = this.getScopeToken(authorization, headerMap);

            boolean ignoreExpiry = config.isIgnoreJwtExpiry();

            String jwt = JwtVerifier.getTokenFromAuthorization(authorization);

            if (jwt != null) {

                if (logger.isTraceEnabled())
                    logger.trace("parsed jwt from authorization = {}", jwt.substring(0, 10));

                try {

                    JwtClaims claims = jwtVerifier.verifyJwt(jwt, ignoreExpiry, true, pathPrefix, reqPath, jwkServiceIds);

                    if (logger.isTraceEnabled())
                        logger.trace("claims = {}", claims.toJson());

                    /* if no auditInfo has been set previously, we populate here */
                    auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                    if (auditInfo == null) {
                        auditInfo = new HashMap<>();
                        exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                    }

                    String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
                    String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
                    String issuer = claims.getStringClaimValue(Constants.ISS);
                    // try to get the cid as some OAuth tokens name it as cid like Okta.
                    if (clientId == null)
                        clientId = claims.getStringClaimValue(Constants.CID);


                    // try to get the uid as some OAuth tokens name it as uid like Okta.
                    if (userId == null)
                        userId = claims.getStringClaimValue(Constants.UID);

                    auditInfo.put(Constants.USER_ID_STRING, userId);
                    auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                    auditInfo.put(Constants.CLIENT_ID_STRING, clientId);
                    auditInfo.put(Constants.ISSUER_CLAIMS, issuer);

                    if (!config.isEnableH2c() && checkForH2CRequest(headerMap)) {
                        Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
                        if (logger.isTraceEnabled()) logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
                        return status;
                    }

                    String callerId = headerMap.getFirst(HttpStringConstants.CALLER_ID);

                    if (callerId != null)
                        auditInfo.put(Constants.CALLER_ID_STRING, callerId);

                    // pass through claims through request headers after verification is done.
                    if(config.getPassThroughClaims() != null && config.getPassThroughClaims().size() > 0) {
                        for(Map.Entry<String, String> entry: config.getPassThroughClaims().entrySet()) {
                            String key = entry.getKey();
                            String header = entry.getValue();
                            Object value = claims.getClaimValue(key);
                            if(logger.isTraceEnabled()) logger.trace("pass through header {} with value {}", header, value);
                            headerMap.put(new HttpString(header), value.toString());
                        }
                    }
                    if (logger.isTraceEnabled())
                        logger.trace("complete SJWT verification for request path = {}", exchange.getRequestURI());

                    if (logger.isDebugEnabled())
                        logger.debug("SimpleJwtVerifyHandler.handleRequest ends.");

                    return null;
                } catch (InvalidJwtException e) {
                    // only log it and unauthorized is returned.
                    logger.error("InvalidJwtException: ", e);
                    Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
                    if (logger.isTraceEnabled())
                        logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                } catch (ExpiredTokenException e) {
                    logger.error("ExpiredTokenException", e);
                    Status status = new Status(STATUS_AUTH_TOKEN_EXPIRED);
                    if (logger.isTraceEnabled())
                        logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                } catch (VerificationException e) {
                    logger.error("VerificationException", e);
                    Status status = new Status(TOKEN_VERIFICATION_EXCEPTION, e.getMessage());
                    if (logger.isTraceEnabled())
                        logger.trace("SimpleJwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                }
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("SimpleJwtVerifyHandler.handleRequest ends with an error.");
                return new Status(STATUS_MISSING_AUTH_TOKEN);            }
        }
    }
    /**
     * Get authToken from X-Scope-Token header.
     * This covers situations where there is a secondary auth token.
     *
     * @param authorization - The auth token from authorization header
     * @param headerMap - complete header map
     * @return - return either x-scope-token or the initial auth token
     */
    protected String getScopeToken(String authorization, HeaderMap headerMap) {
        String returnToken = authorization;
        // in the gateway case, the authorization header might be a basic header for the native API or other authentication headers.
        // this will allow the Basic authentication be wrapped up with a JWT token between proxy client and proxy server for native.
        if (returnToken != null && !returnToken.substring(0, 6).equalsIgnoreCase("Bearer")) {

            // get the jwt token from the X-Scope-Token header in this case and allow the verification done with the secondary token.
            returnToken = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);

            if (logger.isTraceEnabled() && returnToken != null && returnToken.length() > 10)
                logger.trace("The replaced authorization from X-Scope-Token header = {}", returnToken.substring(0, 10));
        }
        return returnToken;
    }


    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnableVerifyJwt();
    }

    @Override
    public JwtVerifier getJwtVerifier() {
        return jwtVerifier;
    }

}
