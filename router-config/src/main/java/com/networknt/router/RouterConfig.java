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
public class RouterConfig {
    private static final Logger logger = LoggerFactory.getLogger(RouterConfig.class);
    static final String CONFIG_NAME = "router";
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
    private static final String SERVICE_ID_QUERY_PARAMETER = "serviceIdQueryParameter";
    private static final String PRE_RESOLVE_FQDN_2_IP = "preResolveFQDN2IP";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";

    boolean http2Enabled;
    boolean httpsEnabled;
    int maxRequestTime;
    Map<String, Integer> pathPrefixMaxRequestTime;
    int connectionsPerThread;
    int softMaxConnectionsPerThread;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;
    int maxQueueSize;


    boolean preResolveFQDN2IP;
    boolean metricsInjection;
    String metricsName;

    List<String> hostWhitelist;
    List<UrlRewriteRule> urlRewriteRules;
    List<MethodRewriteRule> methodRewriteRules;

    Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules;

    Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

    Set httpMethods;
    private Config config;
    private Map<String, Object> mappedConfig;
    boolean serviceIdQueryParameter;

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
        if (mappedConfig.get("urlRewriteRules") != null) {
            if (mappedConfig.get("urlRewriteRules") instanceof String) {
                String s = (String)mappedConfig.get("urlRewriteRules");
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
            } else if (mappedConfig.get("urlRewriteRules") instanceof List) {
                List<String> rules = (List)mappedConfig.get("urlRewriteRules");
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
        if(mappedConfig.get("methodRewriteRules") != null) {
            if (mappedConfig.get("methodRewriteRules") instanceof String) {
                String s = (String)mappedConfig.get("methodRewriteRules");
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
            } else if (mappedConfig.get("methodRewriteRules") instanceof List) {
                List<String> rules = (List)mappedConfig.get("methodRewriteRules");
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
        if(mappedConfig.get("queryParamRewriteRules") != null) {
            if(mappedConfig.get("queryParamRewriteRules") instanceof String) {
                String s = (String)mappedConfig.get("queryParamRewriteRules");
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    Map<String, Object> map = JsonMapper.fromJson(s, Map.class);
                    queryParamRewriteRules = populateQueryParameterRules(map);
                } else {
                    logger.error("queryParamRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
                }
            } else if(mappedConfig.get("queryParamRewriteRules") instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)mappedConfig.get("queryParamRewriteRules");
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
        if(mappedConfig.get("headerRewriteRules") != null) {
            if(mappedConfig.get("headerRewriteRules") instanceof String) {
                String s = (String)mappedConfig.get("headerRewriteRules");
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    Map<String, Object> map = JsonMapper.fromJson(s, Map.class);
                    headerRewriteRules = populateHeaderRewriteRules(map);
                } else {
                    logger.error("headerRewriteRules is the wrong type. Only JSON map or YAML map is supported.");
                }
            } else if(mappedConfig.get("headerRewriteRules") instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)mappedConfig.get("headerRewriteRules");
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
