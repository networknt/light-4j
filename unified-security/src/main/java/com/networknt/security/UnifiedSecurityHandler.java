package com.networknt.security;

import com.networknt.apikey.ApiKeyHandler;
import com.networknt.basicauth.BasicAuthHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is a security handler that combines Anonymous, ApiKey, Basic and OAuth together to avoid all of them
 * to be wired in the request/response chain and skip some of them based on the request path. It allows one
 * path to choose several security handlers at the same time. In most cases, this handler will only be used
 * in a shard light-gateway instance.
 *
 * @author Steve Hu
 */
public class UnifiedSecurityHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(UnifiedSecurityHandler.class);
    static final String BEARER_PREFIX = "BEARER";
    static final String BASIC_PREFIX = "BASIC";
    static final String API_KEY = "apikey";
    static final String JWT = "jwt";
    static final String SWT = "swt";
    static final String SJWT = "sjwt";
    static final String MISSING_AUTH_TOKEN = "ERR10002";
    static final String INVALID_AUTHORIZATION_HEADER = "ERR12003";
    static final String HANDLER_NOT_FOUND = "ERR11200";
    static final String MISSING_PATH_PREFIX_AUTH = "ERR10078";
    static UnifiedSecurityConfig config;
    // make this static variable public so that it can be accessed from the server-info module
    private volatile HttpHandler next;
    public static JwtVerifier jwtVerifier;

    public UnifiedSecurityHandler() {
        logger.info("UnifiedSecurityHandler starts");
        config = UnifiedSecurityConfig.load();
        jwtVerifier = new JwtVerifier(SecurityConfig.load());
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled())
            logger.debug("UnifiedSecurityHandler.handleRequest starts.");
        Status status = verifyUnifiedSecurity(exchange);
        if (status != null) {
            if (logger.isDebugEnabled())
                logger.debug("UnifiedSecurityHandler.handleRequest ends with an error.");
            setExchangeStatus(exchange, status);
            exchange.endExchange();
            return;
        }
        if(logger.isDebugEnabled()) logger.debug("UnifiedSecurityHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    /**
     * Return a status object if there are any error. Otherwise, return null if all checks are passed.
     *
     * @param exchange HttpServerExchange
     * @return Status object if there are any error. Null if there is no error.
     * @throws Exception Exception
     */
    public Status verifyUnifiedSecurity(HttpServerExchange exchange) throws Exception {
        String reqPath = exchange.getRequestPath();
        // check if the path prefix is in the anonymousPrefixes list. If yes, skip all other check and goes to next handler.
        if (config.getAnonymousPrefixes() != null && config.getAnonymousPrefixes().stream().anyMatch(reqPath::startsWith)) {
            if(logger.isTraceEnabled())
                logger.trace("Skip request path base on anonymousPrefixes for " + reqPath);
            return null;
        }
        if(config.getPathPrefixAuths() != null) {
            boolean found = false;
            // iterate each entry to check enabled security methods.
            for(UnifiedPathPrefixAuth pathPrefixAuth: config.getPathPrefixAuths()) {
                if(logger.isTraceEnabled())
                    logger.trace("Check with requestPath = {} prefix = {}", reqPath, pathPrefixAuth.getPrefix());
                if(reqPath.startsWith(pathPrefixAuth.getPrefix())) {
                    found = true;
                    if(logger.isTraceEnabled())
                        logger.trace("Found with requestPath = {} prefix = {}", reqPath, pathPrefixAuth.getPrefix());
                    // check jwt and basic first with authorization header, then check the apikey if it is enabled.
                    if(pathPrefixAuth.isBasic() || pathPrefixAuth.isJwt() || pathPrefixAuth.isSwt() || pathPrefixAuth.isSjwt()) {
                        String authorization = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
                        if(authorization == null) {
                            logger.error("Basic or JWT or SWT or SJWT is enabled and authorization header is missing.");
                            // set the WWW-Authenticate header to Basic realm="realm"
                            if(pathPrefixAuth.isBasic()) {
                                if(logger.isTraceEnabled()) logger.trace("Basic is enabled and set WWW-Authenticate header to Basic realm=\"Default Realm\"");
                                exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "Basic realm=\"Default Realm\"");
                            }
                            if(logger.isDebugEnabled())
                                logger.debug("UnifiedSecurityHandler.handleRequest ends with an error.");
                            return new Status(MISSING_AUTH_TOKEN);
                        } else {
                            // authorization is available. make sure that the length is greater than 5.
                            if(authorization.trim().length() <= 5) {
                                logger.error("Invalid/Unsupported authorization header {}", authorization);
                                return new Status(INVALID_AUTHORIZATION_HEADER, authorization);
                            }
                            // check if it is basic or bearer and handler it differently.
                            if(BASIC_PREFIX.equalsIgnoreCase(authorization.substring(0, 5))) {
                                Map<String, HttpHandler> handlers = Handler.getHandlers();
                                BasicAuthHandler handler = (BasicAuthHandler) handlers.get(BASIC_PREFIX.toLowerCase());
                                if(handler == null) {
                                    logger.error("Cannot find BasicAuthHandler with alias name basic.");
                                    return new Status(HANDLER_NOT_FOUND, "com.networknt.basicauth.BasicAuthHandler@basic");
                                } else {
                                    // if the handler is not enabled in the configuration, break here to call next handler.
                                    if(!handler.isEnabled()) {
                                        break;
                                    }
                                    if(handler.handleBasicAuth(exchange, reqPath, authorization)) {
                                        // verification is passed, go to the next handler in the chain
                                        break;
                                    } else {
                                        // verification is not passed and an error is returned. Don't call the next handler.
                                        // the error is set by the handler.handleBasicAuth method and the exchange is ended.
                                        return null;
                                    }
                                }
                            } else if (BEARER_PREFIX.equalsIgnoreCase(authorization.substring(0, 6))) {
                                // in the case that a bearer token is used, there are three token types: jwt, sjwt and swt. we need to identify the type
                                // and then call the right handler if the type is configured as true.
                                Map<String, HttpHandler> handlers = Handler.getHandlers();
                                // make sure the bearer token exists by checking the length.
                                if(authorization.length() < 8) {
                                    // it has only Bearer and space. return invalid bearer token error
                                    logger.error("Invalid authorization header {}", authorization);
                                    return new Status(INVALID_AUTHORIZATION_HEADER, authorization);
                                }
                                // remove the Bearer prefix to get the token.
                                String token = authorization.substring(7);
                                // first to identify the token type.
                                boolean isJwtOrSjwt = StringUtils.isJwtToken(token);
                                if(logger.isTraceEnabled()) logger.trace("Bearer token is jwt or sjwt = {}", isJwtOrSjwt);
                                if(isJwtOrSjwt) {
                                    // if sjwt is true, need to parse the token to see if it has scope or scp claim to identify if it is a jwt or sjwt.
                                    if(pathPrefixAuth.isSjwt()) {
                                        if(logger.isTraceEnabled()) logger.trace("SJWT is enabled.");
                                        // check if jwt is true.
                                        if(pathPrefixAuth.isJwt()) {
                                            if(logger.isTraceEnabled()) logger.trace("JWT is enabled along with SJWT.");
                                            // we need to check if the token has scope.
                                            boolean isScopeInJwt = jwtVerifier.isScopeInJwt(token, pathPrefixAuth.getPrefix());
                                            if(logger.isTraceEnabled()) logger.trace("Check token has scope = {}", isScopeInJwt);
                                            if(isScopeInJwt) {
                                                // normal jwt, call jwtVerifier to verify the token.
                                                if(logger.isTraceEnabled()) logger.trace("Both jwt and sjwt are true and it has scope. This is a jwt token.");
                                                AbstractJwtVerifyHandler handler = (AbstractJwtVerifyHandler) handlers.get(JWT);
                                                if (handler == null) {
                                                    logger.error("Cannot find JwtVerifyHandler with alias name jwt.");
                                                    return new Status(HANDLER_NOT_FOUND, "com.networknt.openapi.JwtVerifyHandler@jwt");
                                                } else {
                                                    // if the handler is not enabled in the configuration, break here to call next handler.
                                                    if(!handler.isEnabled()) {
                                                        break;
                                                    }
                                                    // get the jwkServiceIds list.
                                                    Status status = handler.handleJwt(exchange, pathPrefixAuth.getPrefix(), reqPath, pathPrefixAuth.getJwkServiceIds());
                                                    if(status != null) {
                                                        return status;
                                                    } else {
                                                        // call the next handler.
                                                        break;
                                                    }
                                                }
                                            } else {
                                                // No scope in the token. It is a sjwt, call sjwtVerifier to verify the token.
                                                if(logger.isTraceEnabled()) logger.trace("Both jwt and sjwt are true and no scope in the token. It is a sjwt token.");
                                                AbstractSimpleJwtVerifyHandler handler = (AbstractSimpleJwtVerifyHandler) handlers.get(SJWT);
                                                if (handler == null) {
                                                    logger.error("Cannot find SimpleJwtVerifyHandler with alias name sjwt.");
                                                    return new Status(HANDLER_NOT_FOUND, "com.networknt.openapi.SimpleJwtVerifyHandler@sjwt");
                                                } else {
                                                    // if the handler is not enabled in the configuration, break here to call next handler.
                                                    if(!handler.isEnabled()) {
                                                        break;
                                                    }
                                                    // get the jwkServiceIds list.
                                                    Status status = handler.handleJwt(exchange, pathPrefixAuth.getPrefix(), reqPath, pathPrefixAuth.getSjwkServiceIds());
                                                    if(status != null) {
                                                        return status;
                                                    } else {
                                                        // verification is passed, go to the next handler in the chain.
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            // jwt is not true. this is must be a sjwt token without scope.
                                            if(logger.isTraceEnabled()) logger.trace("jwt is not true and sjwt is true. This is a sjwt token.");
                                            AbstractSimpleJwtVerifyHandler handler = (AbstractSimpleJwtVerifyHandler) handlers.get(SJWT);
                                            if (handler == null) {
                                                logger.error("Cannot find SimpleJwtVerifyHandler with alias name sjwt.");
                                                return new Status(HANDLER_NOT_FOUND, "com.networknt.openapi.SimpleJwtVerifyHandler@sjwt");
                                            } else {
                                                // if the handler is not enabled in the configuration, break here to call next handler.
                                                if(!handler.isEnabled()) {
                                                    break;
                                                }
                                                // get the jwkServiceIds list.
                                                Status status = handler.handleJwt(exchange, pathPrefixAuth.getPrefix(), reqPath, pathPrefixAuth.getSjwkServiceIds());
                                                if(status != null) {
                                                    return status;
                                                } else {
                                                    // verification is passed, go to the next handler in the chain.
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        // this is just a standard jwt token with scopes.
                                        if(logger.isTraceEnabled()) logger.trace("SJWT is not enabled and this is a jwt token.");
                                        AbstractJwtVerifyHandler handler = (AbstractJwtVerifyHandler) handlers.get(JWT);
                                        if (handler == null) {
                                            logger.error("Cannot find JwtVerifyHandler with alias name jwt.");
                                            return new Status(HANDLER_NOT_FOUND, "com.networknt.openapi.JwtVerifyHandler@jwt");
                                        } else {
                                            // if the handler is not enabled in the configuration, break here to call next handler.
                                            if(!handler.isEnabled()) {
                                                break;
                                            }
                                            // get the jwkServiceIds list.
                                            Status status = handler.handleJwt(exchange, pathPrefixAuth.getPrefix(), reqPath, pathPrefixAuth.getJwkServiceIds());
                                            if(status != null) {
                                                return status;
                                            } else {
                                                // call the next handler.
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    // cannot assume it is a swt token. need to check if swt is enabled for the path prefix.
                                    if(pathPrefixAuth.isSwt()) {
                                        if(logger.isTraceEnabled()) logger.trace("Bearer token is swt.");
                                        AbstractSwtVerifyHandler handler = (AbstractSwtVerifyHandler) handlers.get(SWT);
                                        if (handler == null) {
                                            logger.error("Cannot find SwtVerifyHandler with alias name swt.");
                                            return new Status(HANDLER_NOT_FOUND, "com.networknt.openapi.SwtVerifyHandler@swt");
                                        } else {
                                            // if the handler is not enabled in the configuration, break here to call next handler.
                                            if(!handler.isEnabled()) {
                                                break;
                                            }
                                            // get the jwkServiceIds list.
                                            Status status = handler.handleSwt(exchange, reqPath, pathPrefixAuth.getSwtServiceIds());
                                            if(status != null) {
                                                return status;
                                            } else {
                                                // verification is passed, go to the next handler in the chain.
                                                break;
                                            }
                                        }
                                    } else {
                                        // invalid jwt token.
                                        logger.error("Invalid/Unsupported authorization header {}", authorization);
                                        return new Status(INVALID_AUTHORIZATION_HEADER, authorization);
                                    }
                                }
                            } else {
                                // not BASIC or BEARER, return an error.
                                String s = authorization.length() > 10 ? authorization.substring(0, 10) : authorization;
                                logger.error("Invalid/Unsupported authorization header {}", s);
                                return new Status(INVALID_AUTHORIZATION_HEADER, s);
                            }
                        }
                    } else if (pathPrefixAuth.isApikey()) {
                        Map<String, HttpHandler> handlers = Handler.getHandlers();
                        ApiKeyHandler handler = (ApiKeyHandler) handlers.get(API_KEY);
                        if(handler == null) {
                            logger.error("Cannot find ApiKeyHandler with alias name apikey.");
                            return new Status(HANDLER_NOT_FOUND, "com.networknt.apikey.ApiKeyHandler@apikey");
                        } else {
                            // if the handler is not enabled in the configuration, break here to call next handler.
                            if(!handler.isEnabled()) {
                                break;
                            }
                            if(handler.handleApiKey(exchange, reqPath)) {
                                // the APIKey handler successfully verified the credentials. Need to break here so that the next handler can be called.
                                break;
                            } else {
                                // verification is not passed and an error is returned. need to bypass the next handler.
                                return null;
                            }
                        }

                    }
                }
            }
            if(!found) {
                // cannot find the prefix auth entry for request path.
                logger.error("Cannot find prefix entry in pathPrefixAuths for " + reqPath);
                return new Status(MISSING_PATH_PREFIX_AUTH, reqPath);
            }
        } else {
            // pathPrefixAuths is not defined in the values.yml
            logger.error("Cannot find pathPrefixAuths definition for " + reqPath);
            return new Status(MISSING_PATH_PREFIX_AUTH, reqPath);
        }
        return null;
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(UnifiedSecurityConfig.CONFIG_NAME, UnifiedSecurityHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(UnifiedSecurityConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(UnifiedSecurityConfig.CONFIG_NAME, UnifiedSecurityHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(UnifiedSecurityConfig.CONFIG_NAME), null);
    }

}
