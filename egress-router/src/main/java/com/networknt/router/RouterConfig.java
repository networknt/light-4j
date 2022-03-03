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

    boolean http2Enabled;
    boolean httpsEnabled;
    int maxRequestTime;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;
    List<String> hostWhitelist;
    List<UrlRewriteRule> urlRewriteRules;
    List<MethodRewriteRule> methodRewriteRules;
    Set httpMethods;
    private Config config;
    private final Map<String, Object> mappedConfig;
    boolean serviceIdQueryParameter;

    public RouterConfig() {
        httpMethods = new HashSet();
        httpMethods.add("GET");
        httpMethods.add("POST");
        httpMethods.add("DELETE");
        httpMethods.add("PUT");
        httpMethods.add("PATCH");

        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setHostWhitelist();
        setUrlRewriteRules();
        setMethodRewriteRules();
        setConfigData();

    }

    public static RouterConfig load() {
        return new RouterConfig();
    }

    public void setConfigData() {
        Object object = getMappedConfig().get("http2Enabled");
        if(object != null && (Boolean) object) {
            http2Enabled = true;
        }
        object = getMappedConfig().get("httpsEnabled");
        if(object != null && (Boolean) object) {
            httpsEnabled = true;
        }
        object = getMappedConfig().get("rewriteHostHeader");
        if(object != null && (Boolean) object) {
            rewriteHostHeader = true;
        }
        object = getMappedConfig().get("reuseXForwarded");
        if(object != null && (Boolean) object) {
            reuseXForwarded = true;
        }
        object = getMappedConfig().get("maxRequestTime");
        if(object != null ) {
            maxRequestTime = (Integer)object;
        }
        object = getMappedConfig().get("maxConnectionRetries");
        if(object != null ) {
            maxConnectionRetries = (Integer)object;
        }
        object = getMappedConfig().get("serviceIdQueryParameter");
        if(object != null) {
            serviceIdQueryParameter = (Boolean)object;
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

    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }

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
            urlRewriteRules.add(convertToUrlRewriteRule((String)mappedConfig.get("urlRewriteRules")));
        } else {
            List<String> rules = (List)mappedConfig.get("urlRewriteRules");
            if(rules != null) {
                for (String s : rules) {
                    urlRewriteRules.add(convertToUrlRewriteRule(s));
                }
            }
        }
    }

    private UrlRewriteRule convertToUrlRewriteRule(String s) {
        // make sure that the string has two parts and the first part can be compiled to a pattern.
        String[] parts = StringUtils.split(s, ' ');
        if(parts.length != 2) {
            String error = "The URL rewrite rule " + s + " must have two parts";
            logger.error(error);
            throw new ConfigException(error);
        }
        return new UrlRewriteRule(Pattern.compile(parts[0]), parts[1]);
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
}
