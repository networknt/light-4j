package com.networknt.csrf;

import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This handler provides the subject claim object in auditInfo attachment so that the
 * CSRF handler can get the jwt token payload. It is a very simple implementation to
 * simulate the real JwtVerifyHandler in light-rest-4j, light-graphql-4j and light-hybrid-4j
 * frameworks.
 *
 * @author Steve Hu
 *
 */
public class SimulateJwtVerifierHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(SimulateJwtVerifierHandler.class);

    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";

    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);

    private volatile HttpHandler next;

    public SimulateJwtVerifierHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
        String jwt = JwtHelper.getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtClaims claims = JwtHelper.verifyJwt(jwt);
                Map<String, Object> auditInfo = new HashMap<>();
                auditInfo.put(Constants.ENDPOINT_STRING, exchange.getRequestURI());
                auditInfo.put(Constants.CLIENT_ID_STRING, claims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                auditInfo.put(Constants.USER_ID_STRING, claims.getStringClaimValue(Constants.USER_ID_STRING));
                // This is the id-token scope, it is put into the header for audit and rpc-router for token verification
                // Need to remove the space in order for rpc-router to parse and verify scope
                auditInfo.put(Constants.SCOPE_STRING, claims.getStringListClaimValue(Constants.SCOPE_STRING).toString().replaceAll("\\s+",""));
                auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                exchange.putAttachment(AuditHandler.AUDIT_INFO, auditInfo);
                next.handleRequest(exchange);
            } catch (InvalidJwtException e) {
                // only log it and unauthorized is returned.
                logger.error("Exception: ", e);
                Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(status.toString());
            } catch (ExpiredTokenException e) {
                logger.error("Exception:", e);
                Status status = new Status(STATUS_AUTH_TOKEN_EXPIRED);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(status.toString());
            }
        } else {
            Status status = new Status(STATUS_MISSING_AUTH_TOKEN);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
        }
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
        Object object = config.get(JwtHelper.ENABLE_VERIFY_JWT);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SimulateJwtVerifierHandler.class.getName(), config, null);
    }

}
