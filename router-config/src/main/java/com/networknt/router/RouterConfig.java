/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import com.networknt.handler.config.MethodRewriteRule;
import com.networknt.handler.config.QueryHeaderRewriteRule;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Config class for reverse router.
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "router", configName = "router", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class RouterConfig {
    private static final Logger logger = LoggerFactory.getLogger(RouterConfig.class);
    public static final String CONFIG_NAME = "router";
    private static final String HTTP2_ENABLED = "http2Enabled";
    private static final String HTTPS_ENABLED = "httpsEnabled";
    private static final String REWRITE_HOST_HEADER = "rewriteHostHeader";
    private static final String REUSE_X_FORWARDED = "reuseXForwarded";
    private static final String MAX_REQUEST_TIME = "maxRequestTime";
    private static final String PATH_PREFIX_MAX_REQUEST_TIME = "pathPrefixMaxRequestTime";
    private static final String CONNECTION_PER_THREAD = "connectionsPerThread";
    private static final String SOFT_MAX_CONNECTIONS_PER_THREAD = "softMaxConnectionsPerThread";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";
    private static final String MAX_QUEUE_SIZE = "maxQueueSize";
    private static final String PRE_RESOLVE_FQDN_2_IP = "preResolveFQDN2IP";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    public static final String HOST_WHITE_LIST = "hostWhiteList";
    public static final String HEADER_REWRITE_RULES = "headerRewriteRules";
    public static final String QUERY_PARAM_REWRITE_RULES = "queryParamRewriteRules";
    public static final String METHOD_REWRITE_RULES = "methodRewriteRules";
    public static final String URL_REWRITE_RULES = "urlRewriteRules";
    public static final String SERVICE_ID_QUERY_PARAMETER = "serviceIdQueryParameter";

    @BooleanField(
            configFieldName = HTTP2_ENABLED,
            externalizedKeyName = HTTP2_ENABLED,
            externalized = true,
            defaultValue = true,
            description = "As this router is built to support discovery and security for light-4j services,\n" +
                    "the outbound connection is always HTTP 2.0 with TLS enabled.\n" +
                    "If HTTP 2.0 protocol will be accepted from incoming request."
    )
    boolean http2Enabled;

    @BooleanField(
            configFieldName = HTTPS_ENABLED,
            externalizedKeyName = HTTPS_ENABLED,
            externalized = true,
            defaultValue = true,
            description = "If TLS is enabled when accepting incoming request. Should be true on test and prod."
    )
    boolean httpsEnabled;

    @IntegerField(
            configFieldName = MAX_REQUEST_TIME,
            externalizedKeyName = MAX_REQUEST_TIME,
            externalized = true,
            defaultValue = 1000,
            description = "Max request time in milliseconds before timeout to the server. This is the global setting shared\n" +
                    "by all backend services if they don't have service specific timeout."
    )
    int maxRequestTime;

    @MapField(
            configFieldName = PATH_PREFIX_MAX_REQUEST_TIME,
            externalizedKeyName = PATH_PREFIX_MAX_REQUEST_TIME,
            externalized = true,
            description = "If a particular downstream service has different timeout than the above global definition, you can\n" +
                    "add the path prefix and give it another timeout in millisecond. For downstream APIs not defined here,\n" +
                    "they will use the global timeout defined in router.maxRequestTime. The value is a map with key is the\n" +
                    "path prefix and value is the timeout.\n" +
                    "JSON format:\n" +
                    "router.pathPrefixMaxRequestTime: {\"/v1/address\":5000,\"/v2/address\":10000,\"/v3/address\":30000,\"/v1/pets/{petId}\":5000}\n" +
                    "YAML format:\n" +
                    "router.pathPrefixMaxRequestTime:\n" +
                    "  /v1/address: 5000\n" +
                    "  /v2/address: 10000\n" +
                    "  /v3/address: 30000\n" +
                    "  /v1/pets/{petId}: 5000",
            valueType = Integer.class
    )
    Map<String, Integer> pathPrefixMaxRequestTime;

    @IntegerField(
            configFieldName = CONNECTION_PER_THREAD,
            externalizedKeyName = CONNECTION_PER_THREAD,
            externalized = true,
            defaultValue = 10,
            description = "Connections per thread."
    )
    int connectionsPerThread;

    @IntegerField(
            configFieldName = MAX_QUEUE_SIZE,
            externalizedKeyName = MAX_QUEUE_SIZE,
            externalized = true,
            defaultValue = 0,
            description = "The max queue size for the requests if there is no connection to the downstream API in the connection pool.\n" +
                    "The default value is 0 that means there is queued requests. As we have maxConnectionRetries, there is no\n" +
                    "need to use the request queue to increase the memory usage. It should only be used when you see 503 errors\n" +
                    "in the log after maxConnectionRetries to accommodate slow backend API."
    )
    int maxQueueSize;

    @IntegerField(
            configFieldName = SOFT_MAX_CONNECTIONS_PER_THREAD,
            externalizedKeyName = SOFT_MAX_CONNECTIONS_PER_THREAD,
            externalized = true,
            defaultValue = 5,
            description = "Soft max connections per thread."
    )
    int softMaxConnectionsPerThread;

    @BooleanField(
            configFieldName = REWRITE_HOST_HEADER,
            externalizedKeyName = REWRITE_HOST_HEADER,
            externalized = true,
            defaultValue = true,
            description = "Rewrite Host Header with the target host and port and write X_FORWARDED_HOST with original host"
    )
    boolean rewriteHostHeader;

    @BooleanField(
            configFieldName = REUSE_X_FORWARDED,
            externalizedKeyName = REUSE_X_FORWARDED,
            externalized = true,
            description = "Reuse XForwarded for the target XForwarded header"
    )
    boolean reuseXForwarded;

    @IntegerField(
            configFieldName = MAX_CONNECTION_RETRIES,
            externalizedKeyName = MAX_CONNECTION_RETRIES,
            externalized = true,
            defaultValue = 3,
            description = "Max Connection Retries"
    )
    int maxConnectionRetries;


    @BooleanField(
            configFieldName = PRE_RESOLVE_FQDN_2_IP,
            externalizedKeyName = PRE_RESOLVE_FQDN_2_IP,
            externalized = true,
            description = "Pre-resolve FQDN to IP for downstream connections. Default to false in most case, and it should be\n" +
                    "only used when the downstream FQDN is a load balancer for multiple real API servers."
    )
    boolean preResolveFQDN2IP;

    @ArrayField(
            configFieldName = HOST_WHITE_LIST,
            externalizedKeyName = HOST_WHITE_LIST,
            externalized = true,
            description = "allowed host list. Use Regex to do wildcard match",
            items = String.class
    )
    List<String> hostWhitelist;

    @BooleanField(
            configFieldName = SERVICE_ID_QUERY_PARAMETER,
            externalizedKeyName = SERVICE_ID_QUERY_PARAMETER,
            externalized = true,
            description = "support serviceId in the query parameter for routing to overwrite serviceId in header routing.\n" +
                    "by default, it is false and should not be used unless you are dealing with a legacy client that\n" +
                    "does not support header manipulation. Once this flag is true, we are going to overwrite the header\n" +
                    "service_id derived with other handlers from prefix, path, endpoint etc."
    )
    boolean serviceIdQueryParameter;

    @ArrayField(
            configFieldName = URL_REWRITE_RULES,
            externalizedKeyName = URL_REWRITE_RULES,
            externalized = true,
            description = "URL rewrite rules, each line will have two parts: the regex patten and replace string separated\n" +
                    "with a space. The light-router has service discovery for host routing, so whe working on the\n" +
                    "url rewrite rules, we only need to create about the path in the URL.\n" +
                    "Test your rules at https://www.freeformatter.com/java-regex-tester.html\n" +
                    "Here are some examples in values.yml\n" +
                    "YAML format:\n" +
                    "router.urlRewriteRules:\n" +
                    " /listings/123 to /listing.html?listing=123\n" +
                    " - /listings/(.*)$ /listing.html?listing=$1\n" +
                    " /ph/uat/de-asia-ekyc-service/v1 to /uat-de-asia-ekyc-service/v1\n" +
                    " - /ph/uat/de-asia-ekyc-service/v1 /uat-de-asia-ekyc-service/v1\n" +
                    " /tutorial/linux/wordpress/file1 to /tutorial/linux/cms/file1.php\n" +
                    " - (/tutorial/.*)/wordpress/(\\w+)\\.?.*$ $1/cms/$2.php\n" +
                    "JSON format:\n" +
                    "router.urlRewriteRules: [\"/listings/(.*)$ /listing.html?listing=$1\",\"/ph/uat/de-asia-ekyc-service/v1 /uat-de-asia-ekyc-service/v1\",\"(/tutorial/.*)/wordpress/(\\\\w+)\\\\.?.*$ $1/cms/$2.php\"]",
            items = String.class
    )
    List<UrlRewriteRule> urlRewriteRules;

    @ArrayField(
            configFieldName = METHOD_REWRITE_RULES,
            externalizedKeyName = METHOD_REWRITE_RULES,
            externalized = true,
            description = "Method rewrite rules for legacy clients that do not support DELETE, PUT, and PATCH HTTP methods to\n" +
                    "send a request with GET and POST instead. The gateway will rewrite the method from GET to DELETE or\n" +
                    "from POST to PUT or PATCH. This will be set up at the endpoint level to limit the application.\n" +
                    "The format of the rule will be \"endpoint-pattern source-method target-method\". Please refer to test\n" +
                    "values.yml for examples. The endpoint-pattern is a pattern in OpenAPI specification. Examples:\n" +
                    "YAML format:\n" +
                    "router.methodRewriteRules:\n" +
                    " rewrite POST to PUT for path /v2/address\n" +
                    " - /v2/address POST PUT\n" +
                    " rewrite POST to PATCH for path /v1/address\n" +
                    " - /v1/address POST PATCH\n" +
                    " rewrite GET to DELETE for path /v1/address\n" +
                    " - /v1/pets/{petId}/address GET DELETE\n" +
                    "JSON format:\n" +
                    "router.methodRewriteRules: [\"/v2/address POST PUT\",\"/v1/address POST PATCH\",\"/v1/address GET DELETE\",\"/v1/pets/{petId} GET DELETE\"]\n" +
                    "Note: you cannot rewrite a method with a body to a method without a body or vice versa.",
            items = String.class
    )
    List<MethodRewriteRule> methodRewriteRules;

    @ArrayField(
            configFieldName = QUERY_PARAM_REWRITE_RULES,
            externalizedKeyName = QUERY_PARAM_REWRITE_RULES,
            externalized = true,
            description = "Query parameter rewrite rules for client applications that send different query parameter keys or values\n" +
                    "than the target server expecting. When overwriting a value, the key must be specified in order to\n" +
                    "identify the right query parameter. If only the oldK and newK are specified, the router will rewrite the\n" +
                    "query parameter key oldK with different key newK and keep the value.\n" +
                    "The format of the rules will be a map with the path as the key. Please refer to test values.yml for\n" +
                    "examples. You can define a list of rules under the same path. Here are some examples in values.yml\n" +
                    "YAML format:\n" +
                    "router.queryParamRewriteRules:\n" +
                    " /v1/address\n" +
                    " - oldK: oldV\n" +
                    "   newK: newV\n" +
                    " /v1/pets/{petId}\n" +
                    " - oldK: oldV\n" +
                    "   newK: newV\n" +
                    " - oldK: oldV2\n" +
                    "   newK: newV2\n" +
                    "JSON format:\n" +
                    "router.queryParamRewriteRules: {\"/v1/address\":[{\"oldK\":\"oldV\",\"newK\":\"newV\"}],\"/v1/pets/{petId}\":[{\"oldK\":\"oldV\",\"newK\":\"newV\"},{\"oldK\":\"oldV2\",\"newK\":\"newV2\"}]}",
            items = List.class
    )
    Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules;

    @ArrayField(
            configFieldName = HEADER_REWRITE_RULES,
            externalizedKeyName = HEADER_REWRITE_RULES,
            externalized = true,
            description = "Header rewrite rules for client applications that send different header keys or values than the target\n" +
                    "server expecting. When overwriting a value, the key must be specified in order to identify the right\n" +
                    "header. If only the oldK and newK are specified, the router will rewrite the header key oldK with different\n" +
                    "key newK and keep the value.\n" +
                    "The format of the rule will be a map with the path as the key. Please refer to test values.yml for\n" +
                    "examples. You can define a list of rules under the same path. Here are some examples in values.yml\n" +
                    "YAML format:\n" +
                    "router.headerRewriteRules:\n" +
                    " /v1/address:\n" +
                    " - oldK: oldV\n" +
                    "   newK: newV\n" +
                    " /v1/pets/{petId}:\n" +
                    " - oldK: oldV\n" +
                    "   newK: newV\n" +
                    " - oldK: oldV2\n" +
                    "   newK: newV2\n" +
                    "JSON format:\n" +
                    "router.headerRewriteRules: {\"/v1/address\":[{\"oldK\":\"oldV\",\"newK\":\"newV\"}],\"/v1/pets/{petId}\":[{\"oldK\":\"oldV\",\"newK\":\"newV\"},{\"oldK\":\"oldV2\",\"newK\":\"newV2\"}]}",
            items = List.class
    )
    Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

    @BooleanField(
            configFieldName = METRICS_INJECTION,
            externalizedKeyName = METRICS_INJECTION,
            externalized = true,
            description = "When RouterHandler is used in the http-sidecar or light-gateway, it can collect the metrics info for the\n" +
                    "total response time of the downstream API. With this value injected, users can quickly determine how much\n" +
                    "time the http-sidecar or light-gateway handlers spend and how much time the downstream API spends, including\n" +
                    "the network latency. By default, it is false, and metrics will not be collected and injected into the metrics\n" +
                    "handler configured in the request/response chain."
    )
    boolean metricsInjection;

    @StringField(
            configFieldName = METRICS_NAME,
            externalizedKeyName = METRICS_NAME,
            externalized = true,
            defaultValue = "router-response",
            description = "When the metrics info is injected into the metrics handler, we need to pass a metric name to it so that the\n" +
                    "metrics info can be categorized in a tree structure under the name. By default, it is router-response, and\n" +
                    "users can change it."
    )
    String metricsName;


    Set httpMethods;
    private Config config;
    private Map<String, Object> mappedConfig;


    private RouterConfig() {
        this(CONFIG_NAME);
    }
    private RouterConfig(String configName) {
        httpMethods = new HashSet();
        httpMethods.add("GET");
        httpMethods.add("POST");
        httpMethods.add("DELETE");
        httpMethods.add("PUT");
        httpMethods.add("PATCH");

        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setHostWhitelist();
        setUrlRewriteRules();
        setMethodRewriteRules();
        setQueryParamRewriteRules();
        setHeaderRewriteRules();
        setPathPrefixMaxRequestTime();
    }

    public static RouterConfig load() {
        return new RouterConfig();
    }

    public static RouterConfig load(String configName) {
        return new RouterConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setHostWhitelist();
        setUrlRewriteRules();
        setMethodRewriteRules();
        setQueryParamRewriteRules();
        setHeaderRewriteRules();
        setPathPrefixMaxRequestTime();
    }
    public void setConfigData() {
        Object object = getMappedConfig().get(HTTP2_ENABLED);
        if(object != null) http2Enabled = Config.loadBooleanValue(HTTP2_ENABLED, object);
        object = getMappedConfig().get(HTTPS_ENABLED);
        if(object != null) httpsEnabled = Config.loadBooleanValue(HTTPS_ENABLED, object);
        object = getMappedConfig().get(REWRITE_HOST_HEADER);
        if(object != null) rewriteHostHeader = Config.loadBooleanValue(REWRITE_HOST_HEADER, object);
        object = getMappedConfig().get(REUSE_X_FORWARDED);
        if(object != null) reuseXForwarded = Config.loadBooleanValue(REUSE_X_FORWARDED, object);
        object = getMappedConfig().get(MAX_REQUEST_TIME);
        if(object != null ) maxRequestTime = Config.loadIntegerValue(MAX_REQUEST_TIME, object);
        object = getMappedConfig().get(CONNECTION_PER_THREAD);
        if(object != null ) connectionsPerThread = Config.loadIntegerValue(CONNECTION_PER_THREAD, object);
        object = getMappedConfig().get(SOFT_MAX_CONNECTIONS_PER_THREAD);
        if(object != null ) softMaxConnectionsPerThread = Config.loadIntegerValue(SOFT_MAX_CONNECTIONS_PER_THREAD, object);
        object = getMappedConfig().get(MAX_CONNECTION_RETRIES);
        if(object != null ) maxConnectionRetries = Config.loadIntegerValue(MAX_CONNECTION_RETRIES, object);
        object = getMappedConfig().get(MAX_QUEUE_SIZE);
        if(object != null ) maxQueueSize = Config.loadIntegerValue(MAX_QUEUE_SIZE, object);
        object = getMappedConfig().get(SERVICE_ID_QUERY_PARAMETER);
        if(object != null) serviceIdQueryParameter = Config.loadBooleanValue(SERVICE_ID_QUERY_PARAMETER, object);
        object = getMappedConfig().get(PRE_RESOLVE_FQDN_2_IP);
        if(object != null) preResolveFQDN2IP = Config.loadBooleanValue(PRE_RESOLVE_FQDN_2_IP, object);
        object = getMappedConfig().get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
        object = getMappedConfig().get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }
    public Map<String, Integer> getPathPrefixMaxRequestTime() {
        return pathPrefixMaxRequestTime;
    }
    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }
    public int getSoftMaxConnectionsPerThread() { return softMaxConnectionsPerThread; }
    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }
    public boolean isPreResolveFQDN2IP() { return preResolveFQDN2IP; }
    public boolean isMetricsInjection() { return metricsInjection; }
    public String getMetricsName() { return metricsName; }
    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public int getMaxQueueSize() { return maxQueueSize; }

    public List<String> getHostWhitelist() {
        return hostWhitelist;
    }

    public void setHostWhitelist() {
        this.hostWhitelist = new ArrayList<>();
        if (mappedConfig.get("hostWhitelist") != null) {
            if (mappedConfig.get("hostWhitelist") instanceof String) {
                // multiple host as an JSON list.
                String s = (String)mappedConfig.get("hostWhitelist");
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json list
                    hostWhitelist = (List)JsonMapper.fromJson(s, List.class);
                } else {
                    // single host as a string
                    hostWhitelist.add((String) mappedConfig.get("hostWhitelist"));
                }
            } else {
                hostWhitelist = (List)mappedConfig.get("hostWhitelist");
            }
        }
    }

    public void setHostWhitelist(List<String> hostWhitelist) {
        this.hostWhitelist = hostWhitelist;
    }

    public List<UrlRewriteRule> getUrlRewriteRules() {
        return urlRewriteRules;
    }

    public void setUrlRewriteRules() {
        this.urlRewriteRules = new ArrayList<>();
        if (mappedConfig.get(URL_REWRITE_RULES) != null) {
            if (mappedConfig.get(URL_REWRITE_RULES) instanceof String) {
                String s = (String)mappedConfig.get(URL_REWRITE_RULES);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                // There are two formats for the urlRewriteRules. One is a string separated by a space
                // and the other is a list of strings separated by a space in JSON list format.
                if(s.startsWith("[")) {
                    // multiple rules
                    List<String> rules = (List<String>)JsonMapper.fromJson(s, List.class);
                    for (String rule : rules) {
                        urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(rule));
                    }
                } else {
                    // single rule
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            } else if (mappedConfig.get(URL_REWRITE_RULES) instanceof List) {
                List<String> rules = (List)mappedConfig.get(URL_REWRITE_RULES);
                for (String s : rules) {
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            }
        }
    }

    public void setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
        this.urlRewriteRules = urlRewriteRules;
    }

    public List<MethodRewriteRule> getMethodRewriteRules() {
        return methodRewriteRules;
    }

    public void setMethodRewriteRules() {
        this.methodRewriteRules = new ArrayList<>();
        if(mappedConfig.get(METHOD_REWRITE_RULES) != null) {
            if (mappedConfig.get(METHOD_REWRITE_RULES) instanceof String) {
                String s = (String)mappedConfig.get(METHOD_REWRITE_RULES);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // multiple rules
                    List<String> rules = (List<String>)JsonMapper.fromJson(s, List.class);
                    for (String rule : rules) {
                        methodRewriteRules.add(convertToMethodRewriteRule(rule));
                    }
                } else {
                    // single rule
                    methodRewriteRules.add(convertToMethodRewriteRule(s));
                }
            } else if (mappedConfig.get(METHOD_REWRITE_RULES) instanceof List) {
                List<String> rules = (List)mappedConfig.get(METHOD_REWRITE_RULES);
                for (String s : rules) {
                    methodRewriteRules.add(convertToMethodRewriteRule(s));
                }
            }
        }
    }

    private MethodRewriteRule convertToMethodRewriteRule(String s) {
        // make sure that the string has three parts and the second part and third part are HTTP methods.
        String[] parts = StringUtils.split(s, ' ');
        if(parts.length != 3) {
            String error = "The Method rewrite rule " + s + " must have three parts";
            logger.error(error);
            throw new ConfigException(error);
        }
        String sourceMethod = parts[1].trim().toUpperCase();
        if(!httpMethods.contains(sourceMethod)) {
            String error = "The source method converted to uppercase " + sourceMethod + " is not a valid HTTP Method";
            logger.error(error);
            throw new ConfigException(error);
        }
        String targetMethod = parts[2].trim().toUpperCase();
        if(!httpMethods.contains(targetMethod)) {
            String error = "The target method converted to uppercase " + targetMethod + " is not a valid HTTP Method";
            logger.error(error);
            throw new ConfigException(error);
        }
        return new MethodRewriteRule(parts[0], sourceMethod, targetMethod);
    }

    public void setMethodRewriteRules(List<MethodRewriteRule> methodRewriteRules) {
        this.methodRewriteRules = methodRewriteRules;
    }

    public boolean isServiceIdQueryParameter() {
        return serviceIdQueryParameter;
    }

    public void setServiceIdQueryParameter(boolean serviceIdQueryParameter) {
        this.serviceIdQueryParameter = serviceIdQueryParameter;
    }

    public Map<String, List<QueryHeaderRewriteRule>> getQueryParamRewriteRules() {
        return queryParamRewriteRules;
    }

    public void setQueryParamRewriteRules(Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules) {
        this.queryParamRewriteRules = queryParamRewriteRules;
    }

    public void setQueryParamRewriteRules() {
        queryParamRewriteRules = new HashMap<>();
        if(mappedConfig.get(QUERY_PARAM_REWRITE_RULES) != null) {
            if(mappedConfig.get(QUERY_PARAM_REWRITE_RULES) instanceof String) {
                String s = (String)mappedConfig.get(QUERY_PARAM_REWRITE_RULES);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    Map<String, Object> map = JsonMapper.fromJson(s, Map.class);
                    queryParamRewriteRules = populateQueryParameterRules(map);
                } else {
                    logger.error("queryParamRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
                }
            } else if(mappedConfig.get(QUERY_PARAM_REWRITE_RULES) instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)mappedConfig.get(QUERY_PARAM_REWRITE_RULES);
                queryParamRewriteRules = populateQueryParameterRules(map);
            } else {
                logger.error("queryParamRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
            }
        }
    }

    private Map<String, List<QueryHeaderRewriteRule>> populateQueryParameterRules(Map<String, Object> map) {
        queryParamRewriteRules = new HashMap<>();
        for (Map.Entry<String, Object> r: map.entrySet()) {
            String key =  r.getKey();
            Object object = r.getValue();
            if(object instanceof List) {
                List<QueryHeaderRewriteRule> rules = new ArrayList<>();
                List<Map<String, String>> values = (List<Map<String, String>>)object;
                for(Map<String, String> value: values) {
                    QueryHeaderRewriteRule rule = new QueryHeaderRewriteRule();
                    rule.setOldK(value.get("oldK"));
                    rule.setNewK(value.get("newK"));
                    rule.setOldV(value.get("oldV"));
                    rule.setNewV(value.get("newV"));
                    rules.add(rule);
                }
                queryParamRewriteRules.put(key, rules);
            }
        }
        return queryParamRewriteRules;
    }

    public Map<String, List<QueryHeaderRewriteRule>> getHeaderRewriteRules() {
        return headerRewriteRules;
    }

    public void setHeaderRewriteRules(Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules) {
        this.headerRewriteRules = headerRewriteRules;
    }

    public void setHeaderRewriteRules() {
        headerRewriteRules = new HashMap<>();
        if(mappedConfig.get(HEADER_REWRITE_RULES) != null) {
            if(mappedConfig.get(HEADER_REWRITE_RULES) instanceof String) {
                String s = (String)mappedConfig.get(HEADER_REWRITE_RULES);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    Map<String, Object> map = JsonMapper.fromJson(s, Map.class);
                    headerRewriteRules = populateHeaderRewriteRules(map);
                } else {
                    logger.error("headerRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
                }
            } else if(mappedConfig.get(HEADER_REWRITE_RULES) instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)mappedConfig.get(HEADER_REWRITE_RULES);
                headerRewriteRules = populateHeaderRewriteRules(map);
            } else {
                logger.error("headerRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
            }
        }
    }

    private Map<String, List<QueryHeaderRewriteRule>> populateHeaderRewriteRules(Map<String, Object> map) {
        headerRewriteRules = new HashMap<>();
        for (Map.Entry<String, Object> r: map.entrySet()) {
            String key =  r.getKey();
            Object object = r.getValue();
            if(object instanceof List) {
                List<QueryHeaderRewriteRule> rules = new ArrayList<>();
                List<Map<String, String>> values = (List<Map<String, String>>)object;
                for(Map<String, String> value: values) {
                    QueryHeaderRewriteRule rule = new QueryHeaderRewriteRule();
                    rule.setOldK(value.get("oldK"));
                    rule.setNewK(value.get("newK"));
                    rule.setOldV(value.get("oldV"));
                    rule.setNewV(value.get("newV"));
                    rules.add(rule);
                }
                headerRewriteRules.put(key, rules);
            }
        }
        return headerRewriteRules;
    }

    public void setPathPrefixMaxRequestTime() {
        pathPrefixMaxRequestTime = new HashMap<>();
        if (mappedConfig.get(PATH_PREFIX_MAX_REQUEST_TIME) != null) {
            if (mappedConfig.get(PATH_PREFIX_MAX_REQUEST_TIME) instanceof Map) {
                pathPrefixMaxRequestTime = (Map<String, Integer>)mappedConfig.get(PATH_PREFIX_MAX_REQUEST_TIME);
            } else if (mappedConfig.get(PATH_PREFIX_MAX_REQUEST_TIME) instanceof String) {
                String s = (String)mappedConfig.get(PATH_PREFIX_MAX_REQUEST_TIME);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    try {
                        pathPrefixMaxRequestTime = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    Map<String, Integer> map = new LinkedHashMap<>();
                    for(String keyValue : s.split(" *& *")) {
                        String[] pairs = keyValue.split(" *= *", 2);
                        map.put(pairs[0], pairs.length == 1 ? maxRequestTime : Integer.valueOf(pairs[1])); // use the default maxRequestTime if value is missed.
                    }
                    pathPrefixMaxRequestTime = map;
                }
            } else {
                logger.error("pathPrefixMaxRequestTime is the wrong type. Only JSON map or YAML map is supported.");
            }
        }
    }
}
