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
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractJwtVerifyHandler extends UndertowVerifyHandler implements MiddlewareHandler, IJwtVerifyHandler {
    static final Logger logger = LoggerFactory.getLogger(AbstractJwtVerifyHandler.class);
    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String TOKEN_VERIFICATION_EXCEPTION = "ERR10090";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";
    static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    public static SecurityConfig config;

    // make this static variable public so that it can be accessed from the server-info module
    public static JwtVerifier jwtVerifier;

    public volatile HttpHandler next;

    @Override
    @SuppressWarnings("unchecked")
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        if (logger.isDebugEnabled())
            logger.debug("JwtVerifyHandler.handleRequest starts.");

        if(isSkipAuth(exchange)) {
            if (logger.isDebugEnabled()) logger.debug("Skipped! JwtVerifyHandler.handleRequest ends.");
            Handler.next(exchange, next);
            return;
        }

        String reqPath = exchange.getRequestPath();
        // only UnifiedSecurityHandler will have the jwkServiceIds as the third parameter.
        Status status = handleJwt(exchange, null, reqPath, null);
        if(status != null) {
            setExchangeStatus(exchange, status);
            exchange.endExchange();
        } else {
            if(logger.isDebugEnabled()) logger.debug("JwtVerifyHandler.handleRequest ends.");
            Handler.next(exchange, next);
        }
    }

    /**
     * If jwt verification is correct, return true. Otherwise, return false with ended exchange.
     * @param exchange HttpServerExchange
     * @param pathPrefix path prefix
     * @param reqPath request path
     * @param jwkServiceIds jwk service id list
     * @return status if there is an error. Otherwise, null to indicate no error.
     * @throws Exception exception.
     */
    public Status handleJwt(HttpServerExchange exchange, String pathPrefix, String reqPath, List<String> jwkServiceIds) throws Exception {
        Map<String, Object> auditInfo = null;
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);

        if (logger.isTraceEnabled() && authorization != null && authorization.length() > 10)
            logger.trace("Authorization header = " + authorization.substring(0, 10));
        // if an empty authorization header or a value length less than 6 ("Basic "), return an error
        if(authorization == null ) {
            Status status = new Status(STATUS_MISSING_AUTH_TOKEN);
            if (logger.isTraceEnabled()) logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
            return status;
        } else if(authorization.trim().length() < 6) {
            Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
            if (logger.isTraceEnabled()) logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
            return status;
        } else {
            authorization = this.getScopeToken(authorization, headerMap);

            boolean ignoreExpiry = config.isIgnoreJwtExpiry();

            String jwt = JwtVerifier.getTokenFromAuthorization(authorization);

            if (jwt != null) {

                if (logger.isTraceEnabled())
                    logger.trace("parsed jwt from authorization = " + jwt.substring(0, 10));

                try {

                    JwtClaims claims = jwtVerifier.verifyJwt(jwt, ignoreExpiry, true, pathPrefix, reqPath, jwkServiceIds);

                    if (logger.isTraceEnabled())
                        logger.trace("claims = " + claims.toJson());

                    /* if no auditInfo has been set previously, we populate here */
                    auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                    if (auditInfo == null) {
                        auditInfo = new HashMap<>();
                        exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                    }

                    String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
                    String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
                    String issuer = claims.getStringClaimValue(Constants.ISS_STRING);
                    // try to get the cid as some OAuth tokens name it as cid like Okta.
                    if (clientId == null)
                        clientId = claims.getStringClaimValue(Constants.CID_STRING);


                    // try to get the uid as some OAuth tokens name it as uid like Okta.
                    if (userId == null)
                        userId = claims.getStringClaimValue(Constants.UID_STRING);

                    auditInfo.put(Constants.USER_ID_STRING, userId);
                    auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                    auditInfo.put(Constants.CLIENT_ID_STRING, clientId);
                    auditInfo.put(Constants.ISSUER_CLAIMS, issuer);

                    if (!config.isEnableH2c() && checkForH2CRequest(headerMap)) {
                        Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
                        if (logger.isTraceEnabled()) logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                        return status;
                    }

                    String callerId = headerMap.getFirst(HttpStringConstants.CALLER_ID);

                    if (callerId != null)
                        auditInfo.put(Constants.CALLER_ID_STRING, callerId);

                    if (config != null && config.isEnableVerifyScope()) {
                        if (logger.isTraceEnabled())
                            logger.trace("verify scope from the primary token when enableVerifyScope is true");

                        /* validate scope from operation */
                        String scopeHeader = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);
                        String scopeJwt = JwtVerifier.getTokenFromAuthorization(scopeHeader);
                        List<String> secondaryScopes = new ArrayList<>();

                        Status status = this.hasValidSecondaryScopes(exchange, scopeJwt, secondaryScopes, ignoreExpiry, pathPrefix, reqPath, jwkServiceIds, auditInfo);
                        if(status != null) {
                            if (logger.isTraceEnabled()) logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                            return status;
                        }
                        status = this.hasValidScope(exchange, scopeHeader, secondaryScopes, claims, getSpecScopes(exchange, auditInfo));
                        if(status != null) {
                            if (logger.isDebugEnabled()) logger.debug("JwtVerifyHandler.handleRequest ends with an error {}", status);
                            return status;
                        }
                    }
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
                        logger.trace("complete JWT verification for request path = " + exchange.getRequestURI());

                    if (logger.isDebugEnabled())
                        logger.debug("JwtVerifyHandler.handleRequest ends.");

                    return null;
                } catch (InvalidJwtException e) {
                    // only log it and unauthorized is returned.
                    logger.error("InvalidJwtException: ", e);
                    Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
                    if (logger.isTraceEnabled())
                        logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                } catch (ExpiredTokenException e) {
                    logger.error("ExpiredTokenException", e);
                    Status status = new Status(STATUS_AUTH_TOKEN_EXPIRED);
                    if (logger.isTraceEnabled())
                        logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                } catch (VerificationException e) {
                    logger.error("VerificationException", e);
                    Status status = new Status(TOKEN_VERIFICATION_EXCEPTION, e.getMessage());
                    if (logger.isTraceEnabled())
                        logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                    return status;
                }
            } else {
                Status status = new Status(STATUS_MISSING_AUTH_TOKEN);
                if (logger.isTraceEnabled())
                    logger.trace("JwtVerifyHandler.handleRequest ends with an error {}", status);
                return status;
            }
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
                logger.trace("The replaced authorization from X-Scope-Token header = " + returnToken.substring(0, 10));
        }
        return returnToken;
    }

    /**
     * If the jwt verification will be skipped or not.
     *
     * @param exchange - the current exchange
     * @return - true to skip jwt verification
     */
    public abstract boolean isSkipAuth(HttpServerExchange exchange);


    /**
     * Gets the operation from the spec. If not defined or defined incorrectly, return null.
     *
     * @param exchange - the current exchange
     * @param auditInfo A map of audit info properties
     * @return - return A list of scopes from the spec
     * @throws Exception - exception
     */
    public abstract List<String> getSpecScopes(HttpServerExchange exchange, Map<String, Object> auditInfo) throws Exception;
    /**
     * Check is the request has secondary scopes and they are valid.
     *
     * @param exchange - current exchange
     * @param scopeJwt - the scope found in jwt
     * @param secondaryScopes - Initially an empty list that is then filled with the secondary scopes if there are any.
     * @param ignoreExpiry - if we ignore expiry or not (mostly for testing)
     * @param pathPrefix - request path prefix
     * @param reqPath - the request path as string
     * @param jwkServiceIds - a list of serviceIds for jwk loading
     * @param auditInfo - a map of audit info properties
     * @return - a status if there is an error. or null if there is no error
     */
    protected Status hasValidSecondaryScopes(HttpServerExchange exchange, String scopeJwt, List<String> secondaryScopes, boolean ignoreExpiry, String pathPrefix, String reqPath, List<String> jwkServiceIds, Map<String, Object> auditInfo) {
        if (scopeJwt != null) {
            if (logger.isTraceEnabled())
                logger.trace("start verifying scope token = " + scopeJwt.substring(0, 10));

            try {
                JwtClaims scopeClaims = jwtVerifier.verifyJwt(scopeJwt, ignoreExpiry, true, pathPrefix, reqPath, jwkServiceIds);
                Object scopeClaim = scopeClaims.getClaimValue(Constants.SCOPE_STRING);

                if (scopeClaim instanceof String) {
                    secondaryScopes.addAll(Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCOPE_STRING).split(" ")));
                } else if (scopeClaim instanceof List) {
                    secondaryScopes.addAll(scopeClaims.getStringListClaimValue(Constants.SCOPE_STRING));
                }

                if (secondaryScopes.isEmpty()) {

                    // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                    Object scpClaim = scopeClaims.getClaimValue(Constants.SCP_STRING);
                    if (scpClaim instanceof String) {
                        secondaryScopes.addAll(Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCP_STRING).split(" ")));
                    } else if (scpClaim instanceof List) {
                        secondaryScopes.addAll(scopeClaims.getStringListClaimValue(Constants.SCP_STRING));
                    }
                }
                auditInfo.put(Constants.SCOPE_CLIENT_ID_STRING, scopeClaims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                auditInfo.put(Constants.ACCESS_CLAIMS, scopeClaims);
            } catch (InvalidJwtException e) {
                logger.error("InvalidJwtException", e);
                return new Status(STATUS_INVALID_SCOPE_TOKEN);
            } catch (MalformedClaimException e) {
                logger.error("MalformedClaimException", e);
                return new Status(STATUS_INVALID_AUTH_TOKEN);
            } catch (ExpiredTokenException e) {
                logger.error("ExpiredTokenException", e);
                return new Status(STATUS_SCOPE_TOKEN_EXPIRED);
            }
        }
        return null;
    }

    /**
     * Makes sure the provided scope in the JWT is valid for the main scope or secondary scopes.
     *
     * @param exchange - the current exchange
     * @param scopeHeader - the scope header
     * @param secondaryScopes - list of secondary scopes (can be empty)
     * @param claims - claims found in jwt
     * @param specScopes collect of string scopes
     * @return - null if there is no error. Or status if there is an error.
     */
    protected Status hasValidScope(HttpServerExchange exchange, String scopeHeader, List<String> secondaryScopes, JwtClaims claims, List<String> specScopes) {

        // validate the scope against the scopes configured in the OpenAPI spec
        if (config.isEnableVerifyScope()) {
            if (logger.isTraceEnabled()) logger.trace("validate the scope with the spec scopes {}", specScopes);
            // validate scope
            if (scopeHeader != null) {
                if (logger.isTraceEnabled()) logger.trace("validate the scope with scope token");
                if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                    return new Status(STATUS_SCOPE_TOKEN_SCOPE_MISMATCH, secondaryScopes, specScopes);
                }
            } else {
                // no scope token, verify scope from auth token.
                if (logger.isTraceEnabled()) logger.trace("validate the scope with primary token");
                List<String> primaryScopes = null;
                try {
                    Object scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);
                    if (scopeClaim instanceof String) {
                        primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                    } else if (scopeClaim instanceof List) {
                        primaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
                    }
                    if (primaryScopes == null || primaryScopes.isEmpty()) {
                        // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                        Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);
                        if (scpClaim instanceof String) {
                            primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                        } else if (scpClaim instanceof List) {
                            primaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                        }
                    }
                } catch (MalformedClaimException e) {
                    logger.error("MalformedClaimException", e);
                    return new Status(STATUS_INVALID_AUTH_TOKEN);
                }
                if (!matchedScopes(primaryScopes, specScopes)) {
                    return new Status(STATUS_AUTH_TOKEN_SCOPE_MISMATCH, primaryScopes, specScopes);
                }
            }
        }
        return null;
    }

    protected boolean matchedScopes(List<String> jwtScopes, Collection<String> specScopes) {
        boolean matched = false;
        if (specScopes != null && specScopes.size() > 0) {
            if (jwtScopes != null && jwtScopes.size() > 0) {
                for (String scope : specScopes) {
                    if (jwtScopes.contains(scope)) {
                        matched = true;
                        break;
                    }
                }
            }
        } else {
            matched = true;
        }
        return matched;
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
    public JwtVerifier getJwtVerifier() {
        return jwtVerifier;
    }

}
