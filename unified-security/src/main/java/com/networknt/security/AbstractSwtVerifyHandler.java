package com.networknt.security;

import com.networknt.client.oauth.TokenInfo;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Result;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public abstract class AbstractSwtVerifyHandler extends UndertowVerifyHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(AbstractSwtVerifyHandler.class);
    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";
    static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    static final String STATUS_CLIENT_EXCEPTION = "ERR10082";


    public static SwtVerifier swtVerifier;

    public static SecurityConfig config;
    public volatile HttpHandler next;

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
        return config.isEnableVerifySwt();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("SwtVerifyHandler.handleRequest starts.");

        String reqPath = exchange.getRequestPath();

        // if request path is in the skipPathPrefixes in the config, call the next handler directly to skip the security check.
        if (config.getSkipPathPrefixes() != null && config.getSkipPathPrefixes().stream().anyMatch(reqPath::startsWith)) {
            if(logger.isTraceEnabled())
                logger.trace("Skip request path base on skipPathPrefixes for " + reqPath);
            Handler.next(exchange, next);
            if (logger.isDebugEnabled())
                logger.debug("SwtVerifyHandler.handleRequest ends.");
            return;
        }
        // only UnifiedSecurityHandler will have the jwkServiceIds as the third parameter.
        if(handleSwt(exchange, reqPath, null)) {
            if(logger.isDebugEnabled()) logger.debug("SwtVerifyHandler.handleRequest ends.");
            Handler.next(exchange, next);
        }
    }


    public boolean handleSwt(HttpServerExchange exchange, String reqPath, List<String> jwkServiceIds) throws Exception {
        Map<String, Object> auditInfo = null;
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);

        if (logger.isTraceEnabled() && authorization != null && authorization.length() > 10)
            logger.trace("Authorization header = " + authorization.substring(0, 10));
        // if an empty authorization header or a value length less than 6 ("Basic "), return an error
        if(authorization == null ) {
            setExchangeStatus(exchange, STATUS_MISSING_AUTH_TOKEN);
            exchange.endExchange();
            if (logger.isDebugEnabled()) logger.debug("SwtVerifyHandler.handleRequest ends with an error.");
            return false;
        } else if(authorization.trim().length() < 6) {
            setExchangeStatus(exchange, STATUS_INVALID_AUTH_TOKEN);
            exchange.endExchange();
            if (logger.isDebugEnabled()) logger.debug("SwtVerifyHandler.handleRequest ends with an error.");
            return false;
        } else {
            authorization = this.getScopeToken(authorization, headerMap);
            String swt = SwtVerifier.getTokenFromAuthorization(authorization);
            if (swt != null) {
                if (logger.isTraceEnabled())
                    logger.trace("parsed swt from authorization = " + swt.substring(0, 10));
                String swtClientId = headerMap.getFirst(config.getSwtClientIdHeader());
                String swtClientSecret = headerMap.getFirst(config.getSwtClientSecretHeader());
                if(logger.isTraceEnabled()) logger.trace("header swtClientId = " + swtClientId + ", header swtClientSecret = " + StringUtils.maskHalfString(swtClientSecret));
                Result<TokenInfo> tokenInfoResult = swtVerifier.verifySwt(swt, reqPath, jwkServiceIds, swtClientId, swtClientSecret);
                if(tokenInfoResult.isFailure()) {
                    // return error status to the user.
                    setExchangeStatus(exchange, tokenInfoResult.getError());
                    if (logger.isDebugEnabled()) logger.debug("SwtVerifyHandler.handleRequest ends with an error.");
                    return false;
                }
                TokenInfo tokenInfo = tokenInfoResult.getResult();
                /* if no auditInfo has been set previously, we populate here */
                auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                if (auditInfo == null) {
                    auditInfo = new HashMap<>();
                    exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                }
                String clientId = tokenInfo.getClientId();
                auditInfo.put(Constants.CLIENT_ID_STRING, clientId);
                String issuer = tokenInfo.getIss();
                auditInfo.put(Constants.ISSUER_CLAIMS, issuer);
                if (!config.isEnableH2c() && checkForH2CRequest(headerMap)) {
                    setExchangeStatus(exchange, STATUS_METHOD_NOT_ALLOWED);
                    if (logger.isDebugEnabled()) logger.debug("SwtVerifyHandler.handleRequest ends with an error.");
                    return false;
                }

                String callerId = headerMap.getFirst(HttpStringConstants.CALLER_ID);

                if (callerId != null)
                    auditInfo.put(Constants.CALLER_ID_STRING, callerId);

                if (config != null && config.isEnableVerifyScope()) {
                    if (logger.isTraceEnabled())
                        logger.trace("verify scope from the primary token when enableVerifyScope is true");

                    /* validate scope from operation */
                    String scopeHeader = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);
                    String scopeSwt = SwtVerifier.getTokenFromAuthorization(scopeHeader);
                    List<String> secondaryScopes = new ArrayList<>();

                    if(!this.hasValidSecondaryScopes(exchange, scopeSwt, secondaryScopes, reqPath, jwkServiceIds, auditInfo)) {
                        return false;
                    }
                    if(!this.hasValidScope(exchange, scopeHeader, secondaryScopes, tokenInfo, getSpecScopes(exchange, auditInfo))) {
                        return false;
                    }
                }
                // pass through claims through request headers after verification is done.
                if(config.getPassThroughClaims() != null && config.getPassThroughClaims().size() > 0) {
                    for(Map.Entry<String, String> entry: config.getPassThroughClaims().entrySet()) {
                        String key = entry.getKey();
                        String header = entry.getValue();
                        Field field = tokenInfo.getClass().getDeclaredField(key);
                        field.setAccessible(true);
                        Object value = field.get(tokenInfo);
                        if(logger.isTraceEnabled()) logger.trace("pass through header {} with value {}", header, value);
                        headerMap.put(new HttpString(header), value.toString());
                    }
                }
                if (logger.isTraceEnabled())
                    logger.trace("complete SWT verification for request path = " + exchange.getRequestURI());

                if (logger.isDebugEnabled())
                    logger.debug("SwtVerifyHandler.handleRequest ends.");

                return true;
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("SwtVerifyHandler.handleRequest ends with an error.");
                setExchangeStatus(exchange, STATUS_MISSING_AUTH_TOKEN);
                exchange.endExchange();
                return false;
            }
        }
    }

    /**
     * Makes sure the provided scope in the JWT or SWT is valid for the main scope or secondary scopes.
     *
     * @param exchange - the current exchange
     * @param scopeHeader - the scope header
     * @param secondaryScopes - list of secondary scopes (can be empty)
     * @param tokenInfo - TokenInfo returned from the introspection
     * @param specScopes - a list of scopes
     * @return - return true if scope is valid for endpoint
     */
    protected boolean hasValidScope(HttpServerExchange exchange, String scopeHeader, List<String> secondaryScopes, TokenInfo tokenInfo, List<String> specScopes) {
        // validate the scope against the scopes configured in the OpenAPI spec
        if (config.isEnableVerifyScope()) {

            // validate scope
            if (scopeHeader != null) {
                if (logger.isTraceEnabled()) logger.trace("validate the scope with scope token");
                if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                    setExchangeStatus(exchange, STATUS_SCOPE_TOKEN_SCOPE_MISMATCH, secondaryScopes, specScopes);
                    exchange.endExchange();
                    return false;
                }
            } else {
                // no scope token, verify scope from auth token.
                if (logger.isTraceEnabled()) logger.trace("validate the scope with primary token");
                List<String> primaryScopes = null;
                String scope = tokenInfo.getScope();
                if(scope != null) {
                    primaryScopes = Arrays.asList(scope.split(" "));
                }

                if (!matchedScopes(primaryScopes, specScopes)) {
                    setExchangeStatus(exchange, STATUS_AUTH_TOKEN_SCOPE_MISMATCH, primaryScopes, specScopes);
                    exchange.endExchange();
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean matchedScopes(List<String> tokenScopes, Collection<String> specScopes) {
        boolean matched = false;
        if (specScopes != null && specScopes.size() > 0) {
            if (tokenScopes != null && tokenScopes.size() > 0) {
                for (String scope : specScopes) {
                    if (tokenScopes.contains(scope)) {
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

    /**
     * Check is the request has secondary scopes, and they are valid.
     *
     * @param exchange - current exchange
     * @param scopeSwt - the swt token that associate with a scope
     * @param secondaryScopes - Initially an empty list that is then filled with the secondary scopes if there are any.
     * @param reqPath - the request path as string
     * @param jwkServiceIds - a list of serviceIds for jwk loading
     * @param auditInfo - a map of audit info properties
     * @return - return true if the secondary scopes are valid or if there are no secondary scopes.
     */
    protected boolean hasValidSecondaryScopes(HttpServerExchange exchange, String scopeSwt, List<String> secondaryScopes, String reqPath, List<String> jwkServiceIds, Map<String, Object> auditInfo) {
        if (scopeSwt != null) {
            if (logger.isTraceEnabled())
                logger.trace("start verifying scope token = " + scopeSwt.substring(0, 10));
            try {
                HeaderMap headerMap = exchange.getRequestHeaders();
                String swtClientId = headerMap.getFirst(config.getSwtClientIdHeader());
                String swtClientSecret = headerMap.getFirst(config.getSwtClientSecretHeader());
                if(logger.isTraceEnabled()) logger.trace("header swtClientId = " + swtClientId + ", header swtClientSecret = " + StringUtils.maskHalfString(swtClientSecret));
                Result<TokenInfo> scopeTokenInfo = swtVerifier.verifySwt(scopeSwt, reqPath, jwkServiceIds, swtClientId, swtClientSecret);
                if(scopeTokenInfo.isFailure()) {
                    setExchangeStatus(exchange, scopeTokenInfo.getError());
                    exchange.endExchange();
                    return false;
                }
                TokenInfo tokenInfo = scopeTokenInfo.getResult();
                String scope = tokenInfo.getScope();
                if(scope != null) {
                    secondaryScopes.addAll(Arrays.asList(scope.split(" ")));
                    auditInfo.put(Constants.SCOPE_CLIENT_ID_STRING, tokenInfo.getClientId());
                }
            } catch (Exception e) {
                // only the ClientException is possible here.
                logger.error("Exception", e);
                setExchangeStatus(exchange, STATUS_CLIENT_EXCEPTION, e.getMessage());
                exchange.endExchange();
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the operation from the spec. If not defined or defined incorrectly, return null.
     *
     * @param exchange - the current exchange
     * @param auditInfo A map of audit info properties
     * @return scopes a lst of scopes
     * @throws Exception - exception
     */
    public abstract List<String> getSpecScopes(HttpServerExchange exchange, Map<String, Object> auditInfo) throws Exception;
    /**
     * Get authToken (JWT or SWT) from X-Scope-Token header.
     * This covers situations where there is a secondary auth token.
     *
     * @param authorization - The auth token from authorization header
     * @param headerMap - complete header map
     * @return - return either x-scope-token or the initial auth token
     */
    protected String getScopeToken(String authorization, HeaderMap headerMap) {
        String returnToken = authorization;
        // in the gateway case, the authorization header might be a basic header for the native API or other authentication headers.
        // this will allow the Basic authentication be wrapped up with a JWT or SWT token between proxy client and proxy server for native.
        if (returnToken != null && !returnToken.substring(0, 6).equalsIgnoreCase("Bearer")) {

            // get the swt token from the X-Scope-Token header in this case and allow the verification done with the secondary token.
            returnToken = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);

            if (logger.isTraceEnabled() && returnToken != null && returnToken.length() > 10)
                logger.trace("The replaced authorization from X-Scope-Token header = " + returnToken.substring(0, 10));
        }
        return returnToken;
    }

}
