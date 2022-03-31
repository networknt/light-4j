package com.networknt.limit.key;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

import java.util.Map;

/**
 * This is a customized KeyResolver for one of our customers on the external gateway in the DMZ.
 * There are many external clients that are using the Okta JWT token to access the internal APIs.
 * However, some external clients doesn't support OAuth 2.0, so they will put a client_id and
 * client_secret in the request header to authenticate themselves. So we need to check the JWT
 * token first and then get the client_id from the header second if the JWT doesn't exist.
 *
 * @author Steve Hu
 */
public class JwtHeaderClientIdKeyResolver implements KeyResolver {

    @Override
    public String resolve(HttpServerExchange exchange) {
        String key = null;
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if(auditInfo != null) {
            key = (String)auditInfo.get(Constants.CLIENT_ID_STRING);
        }
        if(key == null) {
            // try to get the key from the header
            HeaderMap headerMap = exchange.getResponseHeaders();
            HeaderValues values = headerMap.get("Client-Id");
            if(values != null) key = values.getFirst();
        }
        return key;
    }
}
