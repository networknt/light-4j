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

package com.networknt.limit;

import com.networknt.config.Config;

import java.util.*;

/**
 * Config class for limit module
 *
 * @author Steve Hu
 */
public class LimitConfig {
    public static final String CONFIG_NAME = "limit";
    ;
    private static final String CONCURRENT_REQUEST = "concurrentRequest";
    private static final String QUEUE_SIZE = "queueSize";
    private static final String ERROR_CODE = "errorCode";
    private static final String LIMIT_KEY = "key";
    private static final String IS_ENABLED = "enabled";
    private static final String RATE_LIMIT = "rateLimit";
    private static final String SERVER = "server";
    private static final String ADDRESS = "address";
    private static final String CLIENT = "client";


    boolean enabled;
    int concurrentRequest;
    int queueSize;
    int errorCode;
    LimitKey key;
    List<LimitQuota> rateLimit;
    Map<String, LimitQuota> server;
    RateLimitSet address;
    RateLimitSet client;
    private  Map<String, Object> mappedConfig;
    private Config config;


    public LimitConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setRateLimitConfig();
    }

    static LimitConfig load() {
        return new LimitConfig();
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConcurrentRequest() {
        return concurrentRequest;
    }

    public void setConcurrentRequest(int concurrentRequest) {
        this.concurrentRequest = concurrentRequest;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public LimitKey getKey() {
        return key;
    }

    public void setKey(LimitKey key) {
        this.key = key;
    }

    public List<LimitQuota> getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(List<LimitQuota> rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Map<String, LimitQuota> getServer() {
        return server;
    }

    public void setServer(Map<String, LimitQuota> server) {
        this.server = server;
    }

    public RateLimitSet getAddress() {
        return address;
    }

    public void setAddress(RateLimitSet address) {
        this.address = address;
    }

    public RateLimitSet getClient() {
        return client;
    }

    public void setClient(RateLimitSet client) {
        this.client = client;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(CONCURRENT_REQUEST);
        if (object != null) {
            concurrentRequest = (int) object;
        }
        object = mappedConfig.get(QUEUE_SIZE);
        if (object != null) {
            queueSize = (int) object;
        }
        object = mappedConfig.get(ERROR_CODE);
        if (object != null) {
            errorCode = (int) object;
        }

        object = getMappedConfig().get(IS_ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
    }

    private void setRateLimitConfig() {
        Object object = mappedConfig.get(LIMIT_KEY);
        if (object != null) {
            key = LimitKey.fromValue((String) object);
        } else {
            key = LimitKey.SERVER;
        }

        object = getMappedConfig().get(RATE_LIMIT);
        if (object != null) {
            String str = (String) object;
            List<String> limits = Arrays.asList(str.split(" "));
            List<LimitQuota> limitQuota = new ArrayList<>();
            limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
            rateLimit = limitQuota;
        }

        if (getMappedConfig().get(SERVER)!=null)  {
            Map<String, String> server_config = (Map<String, String>)getMappedConfig().get(SERVER);
            this.server = new HashMap<>();
            server_config.forEach((k, v)->this.server.put(k, new LimitQuota(v)));
        }

        if (getMappedConfig().get(ADDRESS)!=null)  {
            Map<String, Object> address_config = (Map<String, Object>)getMappedConfig().get(ADDRESS);
            address = new RateLimitSet();

            address_config.forEach((k, o)->{
                if (o instanceof String) {
                    List<String> limits = Arrays.asList(((String)o).split(" "));
                    Map<String, List<LimitQuota>> directMap = new HashMap<>();
                    List<LimitQuota> limitQuota = new ArrayList<>();
                    limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
                    directMap.put(k, limitQuota);
                    address.addDirectMap(directMap);
                } else if (o instanceof Map) {
                    Map<String, String> path = (Map<String, String>)o;
                    Map<String, LimitQuota> pathConfig = new HashMap<>();
                    path.forEach((p, v)->pathConfig.put(p, new LimitQuota(v)));
                    Map<String, Map<String, LimitQuota>> pathMap = new HashMap<>();
                    pathMap.put(k, pathConfig);
                    address.addPathMap(pathMap);
                }
            });

        }

        if (getMappedConfig().get(CLIENT)!=null)  {
            Map<String, Object> client_config = (Map<String, Object>)getMappedConfig().get(CLIENT);
            client = new RateLimitSet();

            client_config.forEach((k, o)->{
                if (o instanceof String) {
                    List<String> limits = Arrays.asList(((String)o).split(" "));
                    Map<String, List<LimitQuota>> directMap = new HashMap<>();
                    List<LimitQuota> limitQuota = new ArrayList<>();
                    limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
                    directMap.put(k, limitQuota);
                    client.addDirectMap(directMap);
                } else if (o instanceof Map) {
                    Map<String, String> path = (Map<String, String>)o;
                    Map<String, LimitQuota> pathConfig = new HashMap<>();
                    path.forEach((p, v)->pathConfig.put(p, new LimitQuota(v)));
                    Map<String, Map<String, LimitQuota>> pathMap = new HashMap<>();
                    pathMap.put(k, pathConfig);
                    client.addPathMap(pathMap);
                }
            });

        }
    }

    class RateLimitSet{
        List<Map<String, List<LimitQuota>>>  directMaps;
        List<Map<String, Map<String,LimitQuota>>>  pathMaps;

        public RateLimitSet() {

        }

        public List<Map<String, List<LimitQuota>>> getDirectMaps() {
            return directMaps;
        }

        public void setDirectMaps(List<Map<String, List<LimitQuota>>> directMaps) {
            this.directMaps = directMaps;
        }

        public void addDirectMap(Map<String, List<LimitQuota>> directMap) {
            if (this.directMaps==null) {
                this.directMaps = new ArrayList<>();
            }
            this.directMaps.add(directMap);
        }

        public List<Map<String, Map<String, LimitQuota>>> getPathMaps() {
            return pathMaps;
        }

        public void setPathMaps(List<Map<String, Map<String, LimitQuota>>> pathMaps) {
            this.pathMaps = pathMaps;
        }

        public void addPathMap(Map<String, Map<String, LimitQuota>> pathMap) {
            if (pathMaps==null) {
                pathMaps = new ArrayList<>();
            }
            this.pathMaps.add(pathMap);
        }
    }
}
