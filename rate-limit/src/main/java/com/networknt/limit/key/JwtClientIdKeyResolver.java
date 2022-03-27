package com.networknt.limit.key;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * When the rate limit handler is located after the JwtVerifierHandler in the request/response chain, we can
 * get the client_id claim from the JWT token from the auditInfo object from the exchange attachment. In this
 * way, we can set up rate limit per client_id to give priority client more access to our services.
 *
 * @author Steve Hu
 */
public class JwtClientIdKeyResolver implements KeyResolver {

    @Override
    public String resolve(HttpServerExchange exchange) {
        String key = null;
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if(auditInfo != null) {
            key = (String)auditInfo.get(Constants.CLIENT_ID_STRING);
        }
        return key;
    }
}
