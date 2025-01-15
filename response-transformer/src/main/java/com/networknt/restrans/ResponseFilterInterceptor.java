package com.networknt.restrans;

import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.http.UndertowConverter;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.utility.ConfigUtils;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.networknt.utility.Constants.ERROR_MESSAGE;

/**
 * This is a response filter interceptor that is used to filter the response body based on the fine-grained authorization
 * configuration for the endpoint. Unlike the transformer interceptor, this interceptor only deals with the response body.
 *
 * The passed in parameter is the original response body and the result is the filtered response body. As there might be
 * several rules to execute in a loop, the previous rule execution result response body will be feed to the next rule in
 * order to perform filter further. The final result will be returned as the response body to the client.
 *
 * @author Steve Hu
 */
public class ResponseFilterInterceptor implements ResponseInterceptor {
    static final Logger logger = LoggerFactory.getLogger(ResponseFilterInterceptor.class);
    private static final String RESPONSE_BODY = "responseBody";
    private static final String METHOD = "method";
    private static final String POST = "post";
    private static final String PUT = "put";
    private static final String PATCH = "patch";
    private static final String REQUEST_BODY = "requestBody";
    private static final String AUDIT_INFO = "auditInfo";
    private static final String STATUS_CODE = "statusCode";

    private static final String STARTUP_HOOK_NOT_LOADED = "ERR11019";
    private static final String RESPONSE_FILTER = "res-fil";
    private static final String PERMISSION = "permission";
    static final String GENERIC_EXCEPTION = "ERR10014";

    private static ResponseFilterConfig config;
    private volatile HttpHandler next;

    /**
     * ResponseFilterInterceptor constructor
     */
    public ResponseFilterInterceptor() {
        if (logger.isInfoEnabled()) logger.info("ResponseFilterInterceptor is loaded");
        config = ResponseFilterConfig.load();
        ModuleRegistry.registerModule(ResponseFilterConfig.CONFIG_NAME, ResponseFilterInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseFilterConfig.CONFIG_NAME), null);
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
        ModuleRegistry.registerModule(ResponseFilterConfig.CONFIG_NAME, ResponseFilterInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseFilterConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(ResponseFilterConfig.CONFIG_NAME, ResponseFilterInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseFilterConfig.CONFIG_NAME), null);
        if(logger.isTraceEnabled()) logger.trace("ResponseFilterInterceptor is reloaded.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.trace("ResponseFilterInterceptor.handleRequest starts.");
        // check the status code. the filter will only be applied to successful request.
        if(exchange.getStatusCode() >= 400) {
            if(logger.isTraceEnabled()) logger.trace("Skip on error code {}.  ResponseFilterInterceptor.handleRequest ends.", exchange.getStatusCode());
            return;
        }
        String requestPath = exchange.getRequestPath();
        if (config.getAppliedPathPrefixes() != null) {
            // check if the path prefix has the second part of encoding to overwrite the defaultBodyEncoding.
            Optional<String> match = findMatchingPrefix(requestPath, config.getAppliedPathPrefixes());
            if(match.isPresent()) {
                // first we need to make sure that the RuleLoaderStartupHook.endpointRules is not empty.
                if(RuleLoaderStartupHook.endpointRules == null) {
                    logger.error("RuleLoaderStartupHook.endpointRules is null. ResponseFilterInterceptor.handlerRequest ends.");
                    // TODO should we replace the response body with an error message to indicate the endpointRules map is empty?
                    return;
                }
                String responseBody = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
                if (logger.isTraceEnabled())
                    logger.trace("original response body = {}", responseBody);

                // call the rule engine to filter the response body. The input contains all the request and response elements.
                HttpString method = exchange.getRequestMethod();
                Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                Map<String, Object> objMap = this.createExchangeInfoMap(exchange, method, responseBody, auditInfo);
                // need to get the rule/rules to execute from the RuleLoaderStartupHook. First, get the endpoint.
                String endpoint, serviceEntry = null;
                if (auditInfo != null) {
                    if (logger.isDebugEnabled())
                        logger.debug("auditInfo exists. Grab endpoint from it.");
                    endpoint = (String) auditInfo.get("endpoint");
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug("auditInfo is NULL. Grab endpoint from exchange.");
                    endpoint = exchange.getRequestPath() + "@" + method.toString().toLowerCase();
                }

                // checked the RuleLoaderStartupHook to ensure it is loaded. If not, return an error to the caller.
                if (RuleLoaderStartupHook.endpointRules == null) {
                    logger.error("RuleLoaderStartupHook endpointRules is null");
                }

                // Grab ServiceEntry from config
                // endpoint = ConfigUtils.toInternalKey(exchange.getRequestMethod().toString().toLowerCase(), exchange.getRequestURI());
                if(logger.isDebugEnabled()) logger.debug("request endpoint: {}", endpoint);
                serviceEntry = ConfigUtils.findServiceEntry(exchange.getRequestMethod().toString().toLowerCase(), exchange.getRequestURI(), RuleLoaderStartupHook.endpointRules);
                if(logger.isDebugEnabled()) logger.debug("request serviceEntry: {}", serviceEntry);

                // get the rules (maybe multiple) based on the endpoint.
                Map<String, List> endpointRules = (Map<String, List>) RuleLoaderStartupHook.endpointRules.get(serviceEntry);
                if (endpointRules == null) {
                    if (logger.isDebugEnabled())
                        logger.debug("endpointRules iS NULL");
                    return;
                } else {
                    // endpointRules is not null.
                    if (logger.isTraceEnabled() && endpointRules.get(RESPONSE_FILTER) != null) {
                        logger.trace("endpointRules size {}", endpointRules.get(RESPONSE_FILTER).size());
                    }
                }

                boolean finalResult = true;
                List<String> responseRules = endpointRules.get(RESPONSE_FILTER);
                if(responseRules == null) {
                    if(logger.isTraceEnabled()) logger.trace("response filter rules is null");
                    return;
                } else {
                    if(logger.isTraceEnabled()) logger.trace("responseRules: {}", responseRules);
                }
                Map<String, Object> result = null;
                for(String ruleId: responseRules) {
                    // copy the col and row objects to the objMap.
                    if(logger.isTraceEnabled()) logger.trace("ruleId: {}", ruleId);
                    Map<String, Object> permissionMap = (Map<String, Object>)endpointRules.get(PERMISSION);
                    if(logger.isTraceEnabled()) logger.trace("permissionMap: {}", permissionMap);
                    if(permissionMap != null) {
                        objMap.put(Constants.COL, permissionMap.get(Constants.COL));
                        objMap.put(Constants.ROW, permissionMap.get(Constants.ROW));
                    }
                    if(logger.isTraceEnabled()) logger.trace("objMap: {}", objMap);
                    result = RuleLoaderStartupHook.ruleEngine.executeRule(ruleId, objMap);
                    boolean res = (Boolean)result.get(RuleConstants.RESULT);
                    if(!res) {
                        finalResult = false;
                        break;
                    }
                    // feed the responseBody from the resultMap to objMap for the next rule.
                    responseBody = (String)result.get(RESPONSE_BODY);
                    if(logger.isTraceEnabled()) logger.trace("responseBody: {}", responseBody);
                    objMap.put(RESPONSE_BODY, responseBody);
                }
                if(finalResult) {
                    responseBody = (String) result.get(RESPONSE_BODY);
                    if (responseBody != null) {
                        // copy transformed buffer to the attachment
                        var dest = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
                        // here we convert back the response body to byte array. Need to find out the default charset.
                        if(logger.isTraceEnabled()) logger.trace("Default Charset {}", Charset.defaultCharset());
                        BuffersUtils.transfer(ByteBuffer.wrap(responseBody.getBytes(StandardCharsets.UTF_8)), dest, exchange);
                    }
                } else {
                    // The finalResult is false to indicate that the rule condition is not met. Return the response as it is.
                    String errorMessage = (String)result.get(ERROR_MESSAGE);
                    if(logger.isTraceEnabled()) logger.trace("Error message {} returns from the plugin", errorMessage);
                }
            }
        }
        if (logger.isDebugEnabled()) logger.trace("ResponseFilterInterceptor.handleRequest ends.");
    }

    private Map<String, Object> createExchangeInfoMap(HttpServerExchange exchange, HttpString method, String responseBody, Map<String, Object> auditInfo) {
        Map<String, Object> objMap = new HashMap<>();
        objMap.put(REQUEST_HEADERS, UndertowConverter.convertHeadersToMap(exchange.getRequestHeaders()));
        objMap.put(RESPONSE_HEADERS, UndertowConverter.convertHeadersToMap(exchange.getResponseHeaders()));
        objMap.put(QUERY_PARAMETERS, UndertowConverter.convertParametersToMap(exchange.getQueryParameters()));
        objMap.put(PATH_PARAMETERS,  UndertowConverter.convertParametersToMap(exchange.getPathParameters()));
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
        return true;
    }
}
