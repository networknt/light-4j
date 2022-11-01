package com.networknt.restrans;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleEngine;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a generic middleware handler to manipulate response based on rule-engine rules so that it can be much more
 * flexible than any other handlers like the header handler to manipulate the headers. The rules will be loaded from
 * the configuration or from the light-portal if portal is implemented.
 *
 * @author Steve Hu
 */
public class ResponseTransformerInterceptor implements ResponseInterceptor {
    static final Logger logger = LoggerFactory.getLogger(ResponseTransformerInterceptor.class);
    private static final String RESPONSE_HEADERS = "responseHeaders";
    private static final String REQUEST_HEADERS = "requestHeaders";
    private static final String RESPONSE_BODY = "responseBody";
    private static final String REMOVE = "remove";
    private static final String UPDATE = "update";
    private static final String QUERY_PARAMETERS = "queryParameters";
    private static final String PATH_PARAMETERS = "pathParameters";
    private static final String METHOD = "method";
    private static final String REQUEST_URL = "requestURL";
    private static final String REQUEST_URI = "requestURI";
    private static final String REQUEST_PATH = "requestPath";
    private static final String POST = "post";
    private static final String PUT = "put";
    private static final String PATCH = "patch";
    private static final String REQUEST_BODY = "requestBody";
    private static final String AUDIT_INFO = "auditInfo";
    private static final String STATUS_CODE = "statusCode";

    private static final String STARTUP_HOOK_NOT_LOADED = "ERR11019";
    private static final String RESPONSE_TRANSFORM = "response-transform";

    private ResponseTransformerConfig config;
    private volatile HttpHandler next;
    private RuleEngine engine;

    public ResponseTransformerInterceptor() {
        if (logger.isInfoEnabled()) logger.info("ResponseManipulatorHandler is loaded");
        config = ResponseTransformerConfig.load();
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
        ModuleRegistry.registerModule(ResponseTransformerInterceptor.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (logger.isTraceEnabled())
            logger.trace("ResponseTransformerHandler.handleRequest is called.");

        String requestPath = exchange.getRequestPath();
        if (config.getAppliedPathPrefixes() != null && config.getAppliedPathPrefixes().stream().anyMatch(requestPath::startsWith)) {
            if (engine == null) {
                engine = new RuleEngine(RuleLoaderStartupHook.rules, null);
            }
            String responseBody = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
            if (logger.isTraceEnabled())
                logger.trace("original response body = " + responseBody);

            // call the rule engine to transform the response body and response headers. The input contains all the request
            // and response elements.
            HttpString method = exchange.getRequestMethod();
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            Map<String, Object> exchangeInfoMap = this.createExchangeInfoMap(exchange, method, responseBody, auditInfo);
            // need to get the rule/rules to execute from the RuleLoaderStartupHook. First, get the endpoint.
            String endpoint;
            if (auditInfo != null) {
                if (logger.isDebugEnabled())
                    logger.debug("auditInfo exists. Grab endpoint from it.");
                endpoint = (String) auditInfo.get("endpoint");
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("auditInfo is NULL. Grab endpoint from exchange.");
                endpoint = exchange.getRequestPath() + "@" + method.toString().toLowerCase();
            }

            if (logger.isDebugEnabled())
                logger.debug("request endpoint: " + endpoint);

            // checked the RuleLoaderStartupHook to ensure it is loaded. If not, return an error to the caller.
            if (RuleLoaderStartupHook.endpointRules == null) {
                logger.error("RuleLoaderStartupHook endpointRules is null");
            }

            // get the rules (maybe multiple) based on the endpoint.
            Map<String, List> endpointRules = (Map<String, List>) RuleLoaderStartupHook.endpointRules.get(endpoint);
            if (endpointRules == null) {
                if (logger.isDebugEnabled())
                    logger.debug("endpointRules iS NULL");
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("endpointRules: " + endpointRules.get(RESPONSE_TRANSFORM).size());
            }

            Map<String, Object> result = this.executeRules(exchangeInfoMap, endpointRules);
            if (result != null) {
                for (Map.Entry<String, Object> entry : result.entrySet()) {

                    if (logger.isTraceEnabled())
                        logger.trace("key = " + entry.getKey() + " value = " + entry.getValue());

                    // you can only update the response headers and response body in the transformation.
                    switch (entry.getKey()) {
                        case RESPONSE_HEADERS:
                            // if responseHeaders object is null, ignore it.
                            Map<String, Object> responseHeaders = (Map) result.get(RESPONSE_HEADERS);
                            if (responseHeaders != null) {
                                // manipulate the response headers.
                                List<String> removeList = (List) responseHeaders.get(REMOVE);
                                if (removeList != null) {
                                    removeList.forEach(s -> exchange.getResponseHeaders().remove(s));
                                }
                                Map<String, Object> updateMap = (Map) responseHeaders.get(UPDATE);
                                if (updateMap != null) {
                                    updateMap.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), (String) v));
                                }
                            }
                            break;
                        case RESPONSE_BODY:
                            responseBody = (String) result.get(RESPONSE_BODY);
                            if (responseBody != null) {
                                // copy transformed buffer to the attachment
                                var dest = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
                                BuffersUtils.transfer(ByteBuffer.wrap(responseBody.getBytes()), dest, exchange);
                            }
                            break;
                    }
                }
            }
        }
    }

    private Map<String, Object> executeRules(Map<String, Object> objMap, Map<String, List> endpointRules) throws Exception {
        // if there is no access rule for this endpoint, check the default deny flag in the config.
        List<Map<String, Object>> responseTransformRules = endpointRules.get(RESPONSE_TRANSFORM);
        Map<String, Object> result = null;
        String ruleId;

        // iterate the rules and execute them in sequence. Break only if one rule is successful.
        for (Map<String, Object> ruleMap : responseTransformRules) {
            ruleId = (String) ruleMap.get(Constants.RULE_ID);
            result = engine.executeRule(ruleId, objMap);
            boolean res = (Boolean) result.get(RuleConstants.RESULT);
            if (!res) {
                return null;
            }
        }
        return result;
    }

    private Map<String, Object> createExchangeInfoMap(HttpServerExchange exchange, HttpString method, String responseBody, Map<String, Object> auditInfo) {
        Map<String, Object> objMap = new HashMap<>();
        objMap.put(REQUEST_HEADERS,  exchange.getRequestHeaders());
        objMap.put(RESPONSE_HEADERS, exchange.getResponseHeaders());
        objMap.put(QUERY_PARAMETERS, exchange.getQueryParameters());
        objMap.put(PATH_PARAMETERS,  exchange.getPathParameters());
        objMap.put(METHOD, method.toString());
        objMap.put(REQUEST_URL, exchange.getRequestURL());
        objMap.put(REQUEST_URI, exchange.getRequestURI());
        objMap.put(REQUEST_PATH, exchange.getRequestPath());
        if (method.toString().equalsIgnoreCase(POST)
                || method.toString().equalsIgnoreCase(PUT)
                || method.toString().equalsIgnoreCase(PATCH)) {
            Object bodyMap = exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
            objMap.put(REQUEST_BODY, bodyMap);
        }
        if (responseBody != null) {
            objMap.put(RESPONSE_BODY, responseBody);
        }
        objMap.put(AUDIT_INFO, auditInfo);
        objMap.put(STATUS_CODE, exchange.getStatusCode());
        return objMap;
    }

    @Override
    public boolean isRequiredContent() {
        return config.isRequiredContent();
    }

}
