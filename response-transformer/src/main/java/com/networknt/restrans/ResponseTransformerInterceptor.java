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
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    public static int MAX_BUFFERS = 1024;
    static final String STARTUP_HOOK_NOT_LOADED = "ERR11019";
    static final String RESPONSE_TRANSFORM = "response-transform";

    private ResponseTransformerConfig config;
    private volatile HttpHandler next;
    private RuleEngine engine;

    public ResponseTransformerInterceptor() {
        if(logger.isInfoEnabled()) logger.info("ResponseManipulatorHandler is loaded");
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

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("ResponseTransformerHandler.handleRequest is called.");
        String requestPath = exchange.getRequestPath();
        if (config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s))) {
            if(engine == null) {
                engine = new RuleEngine(RuleLoaderStartupHook.rules, null);
            }
            String responseBody = null;
            if(!isCompressed(exchange)) {
                responseBody = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
                if(logger.isTraceEnabled()) logger.trace("original response body = " + responseBody);
            }
            // call the rule engine to transform the response body and response headers. The input contains all the request
            // and response elements.
            Map<String, Object> objMap = new HashMap<>();
            objMap.put("requestHeaders", exchange.getRequestHeaders());
            objMap.put("responseHeaders", exchange.getResponseHeaders());
            objMap.put("queryParameters", exchange.getQueryParameters());
            objMap.put("pathParameters", exchange.getPathParameters());
            HttpString method = exchange.getRequestMethod();
            objMap.put("method", method.toString());
            objMap.put("requestURL", exchange.getRequestURL());
            objMap.put("requestURI", exchange.getRequestURI());
            objMap.put("requestPath", exchange.getRequestPath());
            if (method.toString().equalsIgnoreCase("post") || method.toString().equalsIgnoreCase("put") || method.toString().equalsIgnoreCase("patch")) {
                Object bodyMap = exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
                objMap.put("requestBody", bodyMap);
            }
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            objMap.put("auditInfo", auditInfo);
            if(responseBody != null) objMap.put("responseBody", responseBody);
            objMap.put("statusCode", exchange.getStatusCode());
            // need to get the rule/rules to execute from the RuleLoaderStartupHook. First, get the endpoint.
            String endpoint = null;
            if(auditInfo != null) {
            	if(logger.isDebugEnabled()) logger.debug("auditInfo exists. Grab endpoint from it.");
                endpoint = (String) auditInfo.get("endpoint");
            } else {
            	if(logger.isDebugEnabled()) logger.debug("auditInfo is NULL. Grab endpoint from exchange.");
                endpoint = exchange.getRequestPath() + "@" + method.toString().toLowerCase();
            }
            if(logger.isDebugEnabled()) logger.debug("request endpoint: " + endpoint);
            // checked the RuleLoaderStartupHook to ensure it is loaded. If not, return an error to the caller.
            if(RuleLoaderStartupHook.endpointRules == null) {
                logger.error("RuleLoaderStartupHook endpointRules is null");
            }
            // get the rules (maybe multiple) based on the endpoint.
            Map<String, List> endpointRules = (Map<String, List>)RuleLoaderStartupHook.endpointRules.get(endpoint);
            if(endpointRules == null) {
            	if(logger.isDebugEnabled()) 
            		logger.debug("endpointRules iS NULL");
            } else { if(logger.isDebugEnabled()) logger.debug("endpointRules: " + endpointRules.get(RESPONSE_TRANSFORM).size()); }        
            // if there is no access rule for this endpoint, check the default deny flag in the config.
            boolean finalResult = true;
            List<Map<String, Object>> responseTransformRules = endpointRules.get(RESPONSE_TRANSFORM);
            Map<String, Object> result = null;
            String ruleId = null;
            // iterate the rules and execute them in sequence. Break only if one rule is successful.
            for(Map<String, Object> ruleMap: responseTransformRules) {
                ruleId = (String)ruleMap.get(Constants.RULE_ID);
                result = engine.executeRule(ruleId, objMap);
                boolean res = (Boolean)result.get(RuleConstants.RESULT);
                if(!res) {
                    finalResult = false;
                    break;
                }
            }
            if(finalResult) {
                for(Map.Entry<String, Object> entry: result.entrySet()) {
                    if(logger.isTraceEnabled()) logger.trace("key = " + entry.getKey() + " value = " + entry.getValue());
                    // you can only update the response headers and response body in the transformation.
                    switch(entry.getKey()) {
                        case "responseHeaders":
                            // if responseHeaders object is null, ignore it.
                            Map<String, Object> responseHeaders = (Map)result.get("responseHeaders");
                            if(responseHeaders != null) {
                                // manipulate the response headers.
                                List<String> removeList = (List)responseHeaders.get("remove");
                                if(removeList != null) {
                                    removeList.forEach(s -> exchange.getResponseHeaders().remove(s));
                                }
                                Map<String, Object> updateMap = (Map)responseHeaders.get("update");
                                if(updateMap != null) {
                                    updateMap.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), (String)v));
                                }
                            }
                            break;
                        case "responseBody":
                            responseBody = (String)result.get("responseBody");
                            if(responseBody != null && !isCompressed(exchange)) {
                                // change the buffer
                                PooledByteBuffer[] dest = new PooledByteBuffer[MAX_BUFFERS];
                                setBuffer(exchange, dest);
                                BuffersUtils.transfer(ByteBuffer.wrap(responseBody.getBytes()), dest, exchange);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isRequiredContent() {
        return config.isRequiredContent();
    }

    public void setBuffer(HttpServerExchange exchange, PooledByteBuffer[] raw) {
        // close the current buffer pool
        PooledByteBuffer[] oldBuffers = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
        if (oldBuffers != null) {
            for (var oldBuffer: oldBuffers) {
                if (oldBuffer != null) {
                    oldBuffer.close();
                }
            }
        }
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, raw);
    }

    private boolean isCompressed(HttpServerExchange exchange) {
        // check if the request has a header accept encoding with gzip and deflate.
        boolean compressed = false;
        var contentEncodings = exchange.getResponseHeaders().get(Headers.CONTENT_ENCODING_STRING);
        if(contentEncodings != null) {
            for(String values: contentEncodings) {
                if(Arrays.stream(values.split(",")).anyMatch((v) -> Headers.GZIP.toString().equals(v) || Headers.COMPRESS.toString().equals(v) || Headers.DEFLATE.toString().equals(v))) {
                    compressed = true;
                }
            }
        }
        return compressed;
    }

}
