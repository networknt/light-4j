package com.networknt.router;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.utility.HashUtil;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.networknt.httpstring.AttachmentConstants.REQUEST_BODY_STRING;

/**
 * This is a handler to simulate other gateway products to allow consumers to get a client credentials token
 * before sending a request with the authorization header. It will return a dummy token to the consumer app
 * so that we don't need those apps to be modified to avoid the additional cost of migration. When subsequent
 * requests comes in, the header handler will remove the authorization header and the TokenHandler will get
 * a real JWT token from the downstream API authorization server and put it into the Authorization header.
 *
 * This handler is expecting that the RequestBodyInterceptor is used so that we can get the request body in a
 * map structure in the handler.
 *
 * @author Steve Hu
 */
public class OAuthServerHandler implements LightHttpHandler {
    static final Logger logger = LoggerFactory.getLogger(OAuthServerHandler.class);
    private static final String UNSUPPORTED_GRANT_TYPE = "ERR12001";
    private static final String INVALID_BASIC_CREDENTIALS = "ERR12004";

    OAuthServerConfig config;
    public OAuthServerHandler() {
        config = OAuthServerConfig.load();
        if(logger.isInfoEnabled()) logger.info("OAuthServerHandler is loaded.");
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        String requestBody = exchange.getAttachment(REQUEST_BODY_STRING);
        if(logger.isTraceEnabled()) logger.trace("request body = " + requestBody);
        Map<String, Object> formMap = JsonMapper.string2Map(requestBody);
        String clientId = (String)formMap.get("client_id");
        String clientSecret = (String)formMap.get("client_secret");
        String grantType = (String)formMap.get("grant_type");
        if("client_credentials".equals(grantType)) {
            // check if client credentials in the list.
            String credentials = clientId + ":" + clientSecret;
            if(config.getClientCredentials() != null && config.getClientCredentials().stream().anyMatch(credentials::equals)) {
                Map<String, Object> resMap = new HashMap<>();
                resMap.put("access_token", HashUtil.generateUUID());
                resMap.put("token_type", "bearer");
                resMap.put("expires_in", 600);
                if(logger.isTraceEnabled()) logger.trace("matched credential, sending response.");
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseSender().send(JsonMapper.toJson(resMap));
            } else {
                logger.error("invalid credentials");
                setExchangeStatus(exchange, INVALID_BASIC_CREDENTIALS, credentials);
            }
        } else {
            logger.error("grant type is not supported.");
            setExchangeStatus(exchange, UNSUPPORTED_GRANT_TYPE, grantType);
        }
    }
}
