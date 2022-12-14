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
import com.networknt.handler.config.MethodRewriteRule;
import com.networknt.handler.config.QueryHeaderRewriteRule;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

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
    private static final String CONNECTION_PER_THREAD = "connectionsPerThread";
    private static final String SOFT_MAX_CONNECTIONS_PER_THREAD = "softMaxConnectionsPerThread";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";
    private static final String SERVICE_ID_QUERY_PARAMETER = "serviceIdQueryParameter";
    private static final String PRE_RESOLVE_FQDN_2_IP = "preResolveFQDN2IP";

    boolean http2Enabled;
    boolean httpsEnabled;
    int maxRequestTime;
    int connectionsPerThread;
    int softMaxConnectionsPerThread;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;

    boolean preResolveFQDN2IP;
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
        setHostWhitelist();
        setUrlRewriteRules();
        setMethodRewriteRules();
        setQueryParamRewriteRules();
        setHeaderRewriteRules();
        setConfigData();
    }

    public static RouterConfig load() {
        return new RouterConfig();
    }

    public static RouterConfig load(String configName) {
        return new RouterConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setHostWhitelist();
        setUrlRewriteRules();
        setMethodRewriteRules();
        setQueryParamRewriteRules();
        setHeaderRewriteRules();
        setConfigData();
    }
    public void setConfigData() {
        Object object = getMappedConfig().get(HTTP2_ENABLED);
        if(object != null && (Boolean) object) {
            http2Enabled = true;
        }
        object = getMappedConfig().get(HTTPS_ENABLED);
        if(object != null && (Boolean) object) {
            httpsEnabled = true;
        }
        object = getMappedConfig().get(REWRITE_HOST_HEADER);
        if(object != null && (Boolean) object) {
            rewriteHostHeader = true;
        }
        object = getMappedConfig().get(REUSE_X_FORWARDED);
        if(object != null && (Boolean) object) {
            reuseXForwarded = true;
        }
        object = getMappedConfig().get(MAX_REQUEST_TIME);
        if(object != null ) {
            maxRequestTime = (Integer)object;
        }
        object = getMappedConfig().get(CONNECTION_PER_THREAD);
        if(object != null ) {
            connectionsPerThread = (Integer)object;
        }
        object = getMappedConfig().get(SOFT_MAX_CONNECTIONS_PER_THREAD);
        if(object != null ) {
            softMaxConnectionsPerThread = (Integer)object;
        }
        object = getMappedConfig().get(MAX_CONNECTION_RETRIES);
        if(object != null ) {
            maxConnectionRetries = (Integer)object;
        }
        object = getMappedConfig().get(SERVICE_ID_QUERY_PARAMETER);
        if(object != null) {
            serviceIdQueryParameter = (Boolean)object;
        }
        object = getMappedConfig().get(PRE_RESOLVE_FQDN_2_IP);
        if(object != null && (Boolean) object) {
            preResolveFQDN2IP = true;
        }

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
    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }
    public int getSoftMaxConnectionsPerThread() { return softMaxConnectionsPerThread; }
    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }
    public boolean isPreResolveFQDN2IP() { return preResolveFQDN2IP; }

    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public List<String> getHostWhitelist() {
        return hostWhitelist;
    }

    public void setHostWhitelist() {
        this.hostWhitelist = new ArrayList<>();
        if (mappedConfig.get("hostWhitelist") !=null && mappedConfig.get("hostWhitelist") instanceof String) {
            hostWhitelist.add((String)mappedConfig.get("hostWhitelist"));
        } else {
            hostWhitelist = (List)mappedConfig.get("hostWhitelist");
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
        if (mappedConfig.get("urlRewriteRules") !=null && mappedConfig.get("urlRewriteRules") instanceof String) {
            urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule((String)mappedConfig.get("urlRewriteRules")));
        } else {
            List<String> rules = (List)mappedConfig.get("urlRewriteRules");
            if(rules != null) {
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
        if (mappedConfig.get("methodRewriteRules") !=null && mappedConfig.get("methodRewriteRules") instanceof String) {
            methodRewriteRules.add(convertToMethodRewriteRule((String)mappedConfig.get("methodRewriteRules")));
        } else {
            List<String> rules = (List)mappedConfig.get("methodRewriteRules");
            if(rules != null) {
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
        if (mappedConfig.get("queryParamRewriteRules") != null && mappedConfig.get("queryParamRewriteRules") instanceof Map) {
            Map<String, Object> map = (Map<String, Object>)mappedConfig.get("queryParamRewriteRules");
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
        }
    }

    public Map<String, List<QueryHeaderRewriteRule>> getHeaderRewriteRules() {
        return headerRewriteRules;
    }

    public void setHeaderRewriteRules(Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules) {
        this.headerRewriteRules = headerRewriteRules;
    }

    public void setHeaderRewriteRules() {
        headerRewriteRules = new HashMap<>();
        if (mappedConfig.get("headerRewriteRules") != null && mappedConfig.get("headerRewriteRules") instanceof Map) {
            Map<String, Object> map = (Map<String, Object>)mappedConfig.get("headerRewriteRules");
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
        }
    }
}
