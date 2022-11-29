package com.networknt.router;

import com.networknt.client.oauth.Jwt;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.monad.Result;
import com.networknt.router.middleware.TokenHandler;
import com.networknt.utility.HashUtil;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.networknt.httpstring.AttachmentConstants.REQUEST_BODY;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    private static final String CONTENT_TYPE_MISSING = "ERR10076";
    private static final String INVALID_CONTENT_TYPE = "ERR10077";
    private static final String MISSING_AUTHORIZATION_HEADER = "ERR12002";
    private static final String INVALID_AUTHORIZATION_HEADER = "ERR12003";
    OAuthServerConfig config;
    public OAuthServerHandler() {
        config = OAuthServerConfig.load();
        if(logger.isInfoEnabled()) logger.info("OAuthServerHandler is loaded.");
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // response is always application/json.
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        // application/json and x-www-form-urlencoded and form-data are supported.
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if(contentType != null) {
            // only the following three content types are supported.
            if (contentType.startsWith("application/json") || contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded")) {
                Map<String, Object> formMap = (Map<String, Object>)exchange.getAttachment(REQUEST_BODY);
                String clientId = (String)formMap.get("client_id");
                String clientSecret = (String)formMap.get("client_secret");
                String grantType = (String)formMap.get("grant_type");
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
                            resMap.put("access_token", HashUtil.generateUUID());
                            resMap.put("token_type", "bearer");
                            resMap.put("expires_in", 600);
                        }
                        if(logger.isTraceEnabled()) logger.trace("matched credential, sending response.");
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        exchange.getResponseSender().send(JsonMapper.toJson(resMap));
                    } else {
                        logger.error("invalid credentials");
                        setExchangeStatus(exchange, INVALID_BASIC_CREDENTIALS, credentials);
                    }
                } else {
                    logger.error("not supported grant type " + grantType);
                    setExchangeStatus(exchange, UNSUPPORTED_GRANT_TYPE, grantType);
                }
            } else {
                logger.error("invalid content type " + contentType);
                setExchangeStatus(exchange, INVALID_CONTENT_TYPE, contentType);
            }
        } else {
            logger.error("content type is missing and it is required.");
            setExchangeStatus(exchange, CONTENT_TYPE_MISSING);
        }
    }
}
