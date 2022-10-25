package com.networknt.reqtrans;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptor;
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
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Buffers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms the request body of an active request being processed.
 * This is executed by RequestInterceptorExecutionHandler.
 *
 * @author Kalev Gonvick
 *
 */
public class RequestTransformerInterceptor implements RequestInterceptor {
    static final Logger logger = LoggerFactory.getLogger(RequestTransformerInterceptor.class);
    static final String REQUEST_TRANSFORM = "request-transform";

    private RequestTransformerConfig config;
    private volatile HttpHandler next;
    private RuleEngine engine;

    public RequestTransformerInterceptor() {
        if(logger.isInfoEnabled()) logger.info("RequestTransformerHandler is loaded");
        config = RequestTransformerConfig.load();
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
        ModuleRegistry.registerModule(RequestTransformerInterceptor.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("RequestTransformerHandler.handleRequest is called.");
        String requestPath = exchange.getRequestPath();
        if (config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s))) {
            if(engine == null) {
                engine = new RuleEngine(RuleLoaderStartupHook.rules, null);
            }
            String method = exchange.getRequestMethod().toString();
            if (!HttpContinue.requiresContinueResponse(exchange.getRequestHeaders())) {
                if(logger.isDebugEnabled()) logger.debug("request can be transformed since no Expect headers found");

                Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                // checked the RuleLoaderStartupHook to ensure it is loaded. If not, return an error to the caller.
                if(RuleLoaderStartupHook.endpointRules == null) {
                    logger.error("RuleLoaderStartupHook endpointRules is null");
                }
                // need to get the rule/rules to execute from the RuleLoaderStartupHook. First, get the endpoint.
                String endpoint, serviceEntry = null;
                // Grab ServiceEntry from config
                endpoint = ConfigUtils.toInternalKey(exchange.getRequestMethod().toString().toLowerCase(), exchange.getRequestURI());
                if(logger.isDebugEnabled()) logger.debug("request endpoint: " + endpoint);
                serviceEntry = ConfigUtils.findServiceEntry(exchange.getRequestMethod().toString().toLowerCase(), exchange.getRequestURI(), RuleLoaderStartupHook.endpointRules);
                if(logger.isDebugEnabled()) logger.debug("request serviceEntry: " + serviceEntry);

                // get the rules (maybe multiple) based on the endpoint.
                Map<String, List> endpointRules = (Map<String, List>)RuleLoaderStartupHook.endpointRules.get(serviceEntry);
                if(endpointRules == null) {
                    if(logger.isDebugEnabled())
                        logger.debug("endpointRules iS NULL");
                } else { if(logger.isDebugEnabled()) logger.debug("endpointRules: " + endpointRules.get(REQUEST_TRANSFORM).size()); }
                if(endpointRules != null) {
                    List<Map<String, Object>> requestTransformRules = endpointRules.get(REQUEST_TRANSFORM);
                    if(requestTransformRules != null) {
                        boolean finalResult = true;
                        // call the rule engine to transform the request metadata or body. The input contains all the request elements
                        Map<String, Object> objMap = new HashMap<>();
                        objMap.put("auditInfo", auditInfo);
                        objMap.put("requestHeaders", exchange.getRequestHeaders());
                        objMap.put("responseHeaders", exchange.getRequestHeaders());
                        objMap.put("queryParameters", exchange.getQueryParameters());
                        objMap.put("pathParameters", exchange.getPathParameters());
                        objMap.put("method", method);
                        objMap.put("requestURL", exchange.getRequestURL());
                        objMap.put("requestURI", exchange.getRequestURI());
                        objMap.put("requestPath", exchange.getRequestPath());
                        if ((method.equalsIgnoreCase("post") || method.equalsIgnoreCase("put") || method.equalsIgnoreCase("patch")) && !exchange.isRequestComplete()) {
                            // This object contains the reference to the request data buffer. Any modification done to this will be reflected in the request.
                            PooledByteBuffer[] requestData = this.getBuffer(exchange);
                            String s = BuffersUtils.toString(requestData, StandardCharsets.UTF_8);
                            // Transform the request body with the rule engine.
                            if(logger.isDebugEnabled()) logger.debug("original request body = " + s);
                            objMap.put("requestBody", s);
                        }
                        Map<String, Object> result = null;
                        String ruleId = null;
                        // iterate the rules and execute them in sequence. Break only if one rule is successful.
                        if(logger.isDebugEnabled()) logger.debug("requestTransformRules list count: " + requestTransformRules.size());
                        for(Map<String, Object> ruleMap: requestTransformRules) {
                            ruleId = (String)ruleMap.get(Constants.RULE_ID);
                            if(logger.isDebugEnabled()) logger.debug("ruleID found: " + ruleId);
                            result = engine.executeRule(ruleId, objMap);
                            boolean res = (Boolean)result.get(RuleConstants.RESULT);
                            if(logger.isDebugEnabled() && res) logger.debug("ruleID result is true");
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
                                    case "requestPath":
                                        String reqPath = (String)result.get("requestPath");
                                        exchange.setRequestPath(reqPath);
                                        if(logger.isTraceEnabled()) logger.trace("requestPath is changed to " + reqPath);
                                        break;
                                    case "requestURI":
                                        String requestURI = (String)result.get("requestURI");
                                        exchange.setRequestURI(requestURI);
                                        break;
                                    case "requestHeaders":
                                        // if requestHeaders object is null, ignore it.
                                        Map<String, Object> requestHeaders = (Map)result.get("requestHeaders");
                                        if(requestHeaders != null) {
                                            // manipulate the request headers.
                                            List<String> removeList = (List)requestHeaders.get("remove");
                                            if(removeList != null) {
                                                removeList.forEach(s -> exchange.getRequestHeaders().remove(s));
                                            }
                                            Map<String, Object> updateMap = (Map)requestHeaders.get("update");
                                            if(updateMap != null) {
                                                updateMap.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), (String)v));
                                            }
                                        }
                                        break;
                                    case "requestBody":
                                        String s = (String)result.get("requestBody");
                                        ByteBuffer overwriteData = ByteBuffer.wrap(s.getBytes());
                                        PooledByteBuffer[] requestData = this.getBuffer(exchange);
                                        // Do the overwrite operation by copying our overwriteData to the source buffer pool.
                                        int pidx = 0;
                                        while (overwriteData.hasRemaining() && pidx < requestData.length) {
                                            ByteBuffer _dest;
                                            if (requestData[pidx] == null) {
                                                requestData[pidx] = exchange.getConnection().getByteBufferPool().allocate();
                                                _dest = requestData[pidx].getBuffer();
                                            } else {
                                                _dest = requestData[pidx].getBuffer();
                                                _dest.clear();
                                            }
                                            Buffers.copy(_dest, overwriteData);
                                            _dest.flip();
                                            pidx++;
                                        }
                                        while (pidx < requestData.length) {
                                            requestData[pidx] = null;
                                            pidx++;
                                        }

                                        // We need to update the content length.
                                        long length = 0;
                                        for (PooledByteBuffer dest : requestData) {
                                            if (dest != null) {
                                                length += dest.getBuffer().limit();
                                            }
                                        }
                                        exchange.getRequestHeaders().put(Headers.CONTENT_LENGTH, length);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isRequiredContent() {
        return config.isRequiredContent();
    }

    public PooledByteBuffer[] getBuffer(HttpServerExchange exchange) {
        PooledByteBuffer[] buffer = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        if (buffer == null) {
            throw new IllegalStateException("Request content is not available in exchange attachment as there is no interceptors.");
        }
        return buffer;
    }
}
