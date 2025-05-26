package com.networknt.router;

import com.networknt.client.oauth.Jwt;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.monad.Result;
import com.networknt.router.middleware.TokenHandler;
import com.networknt.utility.HashUtil;
import com.networknt.utility.UuidUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.networknt.httpstring.AttachmentConstants.REQUEST_BODY;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Warning: This is a handler that should never be used. Putting the client secret in the query parameters is
 * a very bad idea. And it is against the OAuth 2.0 spec. This handler is only used for testing and migrating.
 *
 * This is a handler to simulate other gateway products to allow consumers to get a client credentials token
 * before sending a request with the authorization header. It will return a dummy token to the consumer app
 * so that we don't need those apps to be modified to avoid the additional cost of migration. When subsequent
 * requests comes in, the header handler will remove the authorization header and the TokenHandler will get
 * a real JWT token from the downstream API authorization server and put it into the Authorization header.
 *
 * @author Steve Hu
 */
public class OAuthServerGetHandler implements LightHttpHandler {
    static final Logger logger = LoggerFactory.getLogger(OAuthServerGetHandler.class);
    private static final String METHOD_NOT_ALLOWED = "ERR10008";
    private static final String UNSUPPORTED_GRANT_TYPE = "ERR12001";
    private static final String INVALID_BASIC_CREDENTIALS = "ERR12004";
    private static final String CONTENT_TYPE_MISSING = "ERR10076";
    private static final String INVALID_CONTENT_TYPE = "ERR10077";
    private static final String MISSING_AUTHORIZATION_HEADER = "ERR12002";
    private static final String INVALID_AUTHORIZATION_HEADER = "ERR12003";
    OAuthServerConfig config;
    public OAuthServerGetHandler() {
        config = OAuthServerConfig.load();
        if(logger.isInfoEnabled()) logger.info("OAuthServerGetHandler is loaded and it is not secure!!!");
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // check the config to see if this handler is enabled.
        if(!config.isGetMethodEnabled()) {
            setExchangeStatus(exchange, METHOD_NOT_ALLOWED, exchange.getRequestMethod().toString(), exchange.getRequestURI());
            return;
        }
        // response is always application/json.
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        // get the query parameters from the exchange.
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
        String clientId = null;
        String clientSecret = null;
        String grantType = null;
        if(queryParameters != null) {
            clientId = queryParameters.get("client_id") != null?queryParameters.get("client_id").getFirst():null;
            clientSecret = queryParameters.get("client_secret") != null?queryParameters.get("client_secret").getFirst():null;
            grantType = queryParameters.get("grant_type") != null?queryParameters.get("grant_type").getFirst():null;
        }
        if("client_credentials".equals(grantType)) {
            // check if client credentials in the list.
            String credentials = null;
            if(clientId != null && clientSecret != null) {
                credentials = clientId + ":" + clientSecret;
            } else {
                // clientId and clientSecret are not in the body but in the authorization header.
                String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
                if(auth != null) {
                    if("BASIC".equalsIgnoreCase(auth.substring(0, 5))) {
                        credentials = auth.substring(6);
                        int pos = credentials.indexOf(':');
                        if (pos == -1) {
                            credentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(credentials), UTF_8);
                        }
                    } else {
                        logger.error("Invalid authorization header " + auth.substring(0, 10));
                        setExchangeStatus(exchange, INVALID_AUTHORIZATION_HEADER, auth.substring(0, 10));
                        return;
                    }
                } else {
                    // missing credentials.
                    logger.error("Missing authorization header.");
                    setExchangeStatus(exchange, MISSING_AUTHORIZATION_HEADER);
                    return;
                }
            }
            if(config.getClientCredentials() != null && config.getClientCredentials().stream().anyMatch(credentials::equals)) {
                Map<String, Object> resMap = new HashMap<>();

                if(config.isPassThrough()) {
                    // get a jwt token from the real OAuth 2.0 provider.
                    Result<Jwt> result = TokenHandler.getJwtToken(config.getTokenServiceId());
                    if(result.isFailure()) {
                        logger.error("Cannot populate or renew jwt for client credential grant type: " + result.getError().toString());
                        setExchangeStatus(exchange, result.getError());
                        return;
                    } else {
                        Jwt jwt = result.getResult();
                        resMap.put("access_token", jwt.getJwt());
                        resMap.put("token_type", "bearer");
                        resMap.put("expires_in", (jwt.getExpire() - System.currentTimeMillis()) / 1000); // milliseconds to seconds.
                    }
                } else {
                    // generate a dummy token just to complete the consumer workflow without code change.
                    resMap.put("access_token", UuidUtil.uuidToBase64(UuidUtil.getUUID()));
                    resMap.put("token_type", "bearer");
                    resMap.put("expires_in", 600);
                }
                if(logger.isTraceEnabled()) logger.trace("matched credential, sending response.");
                exchange.getResponseSender().send(JsonMapper.toJson(resMap));
            } else {
                logger.error("invalid credentials");
                setExchangeStatus(exchange, INVALID_BASIC_CREDENTIALS, credentials);
            }
        } else {
            logger.error("not supported grant type " + grantType);
            setExchangeStatus(exchange, UNSUPPORTED_GRANT_TYPE, grantType);
        }
    }
}
