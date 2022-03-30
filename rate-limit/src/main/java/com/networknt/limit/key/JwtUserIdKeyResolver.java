package com.networknt.limit.key;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * When user is selected as the key, we can get the user_id from the JWT claim. In this way, we
 * can limit a number of requests for a user to prevent abuse from a single page application that
 * is using the backend APIs.
 *
 * @author Steve Hu
 *
 */
public class JwtUserIdKeyResolver implements KeyResolver {

    @Override
    public String resolve(HttpServerExchange exchange) {
        String key = null;
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if(auditInfo != null) {
            key = (String)auditInfo.get(Constants.USER_ID_STRING);
        }
        return key;
    }
}
