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
        exchange.startBlocking();
        String requestPath = exchange.getRequestPath();
        if (config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s))) {
            if(engine == null) {
                engine = new RuleEngine(RuleLoaderStartupHook.rules, null);
            }
            String s = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
            if(logger.isTraceEnabled()) logger.trace("original response body = " + s);
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
            objMap.put("responseBody", s);
            objMap.put("statusCode", exchange.getStatusCode());
            // need to get the rule/rules to execute from the RuleLoaderStartupHook. First, get the endpoint.
            String endpoint = null;
            if(auditInfo != null) {
                endpoint = (String) auditInfo.get("endpoint");
            } else {
                endpoint = exchange.getRequestPath() + "@" + method.toString().toLowerCase();
            }
            // checked the RuleLoaderStartupHook to ensure it is loaded. If not, return an error to the caller.
            if(RuleLoaderStartupHook.endpointRules == null) {
                logger.error("RuleLoaderStartupHook endpointRules is null");
            }
            // get the rules (maybe multiple) based on the endpoint.
            Map<String, List> endpointRules = (Map<String, List>)RuleLoaderStartupHook.endpointRules.get(endpoint);
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

                            break;
                        case "responseBody":
                            s = (String)result.get("responseBody");
                            // change the buffer
                            PooledByteBuffer[] dest = new PooledByteBuffer[MAX_BUFFERS];
                            setBuffer(exchange, dest);
                            BuffersUtils.transfer(ByteBuffer.wrap(s.getBytes()), dest, exchange);
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

}
