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
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Config class for limit module
 *
 * @author Steve Hu
 */
public class LimitConfig {
    private static final Logger logger = LoggerFactory.getLogger(LimitConfig.class);
    public static final String CONFIG_NAME = "limit";
    ;
    private static final String CONCURRENT_REQUEST = "concurrentRequest";
    private static final String QUEUE_SIZE = "queueSize";
    private static final String ERROR_CODE = "errorCode";
    private static final String LIMIT_KEY = "key";
    private static final String IS_ENABLED = "enabled";
    private static final String CLIENT_ID_KEY = "clientIdKeyResolver";
    private static final String USER_ID_KEY = "userIdKeyResolver";
    private static final String ADDRESS_KEY = "addressKeyResolver";
    private static final String RATE_LIMIT = "rateLimit";
    private static final String SERVER = "server";
    private static final String ADDRESS = "address";
    private static final String CLIENT = "client";
    private static final String USER = "user";
    public static final String SEPARATE_KEY = "#";


    boolean enabled;
    int concurrentRequest;
    int queueSize;
    int errorCode;
    String clientIdKeyResolver;
    String addressKeyResolver;
    String userIdKeyResolver;

    LimitKey key;
    List<LimitQuota> rateLimit;
    Map<String, LimitQuota> server;
    RateLimitSet address;
    RateLimitSet client;
    RateLimitSet user;
    private  Map<String, Object> mappedConfig;
    private final Config config;


    private LimitConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private LimitConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setRateLimitConfig();
    }

    public static LimitConfig load() {
        return new LimitConfig();
    }

    public static LimitConfig load(String configName) {
        return new LimitConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setRateLimitConfig();
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

    public String getClientIdKeyResolver() {
        return clientIdKeyResolver;
    }

    public void setClientIdKeyResolver(String clientIdKeyResolver) {
        this.clientIdKeyResolver = clientIdKeyResolver;
    }

    public String getAddressKeyResolver() {
        return addressKeyResolver;
    }

    public void setAddressKeyResolver(String addressKeyResolver) {
        this.addressKeyResolver = addressKeyResolver;
    }

    public String getUserIdKeyResolver() {
        return userIdKeyResolver;
    }

    public void setUserIdKeyResolver(String userIdKeyResolver) {
        this.userIdKeyResolver = userIdKeyResolver;
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

    public RateLimitSet getUser() {
        return user;
    }

    public void setUser(RateLimitSet user) {
        this.user = user;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(CONCURRENT_REQUEST);
        if (object != null) concurrentRequest = Config.loadIntegerValue(CONCURRENT_REQUEST, object);
        object = mappedConfig.get(QUEUE_SIZE);
        if (object != null) queueSize = Config.loadIntegerValue(QUEUE_SIZE, object);
        object = mappedConfig.get(ERROR_CODE);
        if (object != null) {
            errorCode = Config.loadIntegerValue(ERROR_CODE, object);
        } else {
            // set default value to 503.
            errorCode = 503;
        }

        object = getMappedConfig().get(IS_ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(IS_ENABLED, object);
        object = getMappedConfig().get(CLIENT_ID_KEY);
        if(object != null) setClientIdKeyResolver((String) object);
        object = getMappedConfig().get(ADDRESS_KEY);
        if(object != null) setAddressKeyResolver((String) object);
        object = getMappedConfig().get(USER_ID_KEY);
        if(object != null) setUserIdKeyResolver((String) object);
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
        } else {
            // if rateLimit doesn't exist, use the concurrentRequest as request per second.
            List<LimitQuota> limitQuota = new ArrayList<>();
            limitQuota.add(new LimitQuota(concurrentRequest, TimeUnit.SECONDS));
            rateLimit = limitQuota;
        }

        if (mappedConfig.get(SERVER)!=null)  {
            Object serverObject = mappedConfig.get(SERVER);
            if(serverObject != null) {
                this.server = new HashMap<>();
                if(serverObject instanceof String) {
                    String s = (String) serverObject;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("server s = " + s);
                    if(s.startsWith("{")) {
                        Map<String, Object> serverConfig = JsonMapper.string2Map(s);
                        serverConfig.forEach((k, v)->this.server.put(k, new LimitQuota((String)v)));
                    } else {
                        logger.error("server is the wrong type. Only JSON map or YAML map is supported.");
                    }
                } else if(serverObject instanceof Map) {
                    Map<String, String> serverConfig = (Map<String, String>) serverObject;
                    serverConfig.forEach((k, v)->this.server.put(k, new LimitQuota(v)));
                } else {
                    logger.error("server is the wrong type. Only JSON map or YAML map is supported.");
                }
            }
        }

        if (mappedConfig.get(ADDRESS)!=null)  {
            Object addressObject = mappedConfig.get(ADDRESS);
            if(addressObject != null) {
                address = new RateLimitSet();
                if (addressObject instanceof String) {
                    String s = (String) addressObject;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("address s = " + s);
                    if(s.startsWith("{")) {
                        Map<String, Object> addressConfig = JsonMapper.string2Map(s);
                        address = populateFromMap(addressConfig);
                    } else {
                        logger.error("address is the wrong type. Only JSON map or YAML map is supported.");
                    }
                } else if(addressObject instanceof Map) {
                    Map<String, Object> addressConfig = (Map<String, Object>) addressObject;
                    address = populateFromMap(addressConfig);
                } else {
                    logger.error("address is the wrong type. Only JSON map or YAML map is supported.");
                }
            }
        }

        if (mappedConfig.get(CLIENT)!=null)  {
            Object clientObject = mappedConfig.get(CLIENT);
            if (clientObject != null) {
                client = new RateLimitSet();
                if (clientObject instanceof String) {
                    String s = (String) clientObject;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("client s = " + s);
                    if(s.startsWith("{")) {
                        Map<String, Object> clientConfig = JsonMapper.string2Map(s);
                        client = populateFromMap(clientConfig);
                    } else {
                        logger.error("client is the wrong type. Only JSON map or YAML map is supported.");
                    }
                } else if(clientObject instanceof Map) {
                    Map<String, Object> clientConfig = (Map<String, Object>) clientObject;
                    client = populateFromMap(clientConfig);
                } else {
                    logger.error("client is the wrong type. Only JSON map or YAML map is supported.");
                }
            }
        }

        if (mappedConfig.get(USER)!=null)  {
            Object userObject = mappedConfig.get(USER);
            if(userObject != null) {
                user = new RateLimitSet();
                if (userObject instanceof String) {
                    String s = (String) userObject;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("user s = " + s);
                    if(s.startsWith("{")) {
                        Map<String, Object> userConfig = JsonMapper.string2Map(s);
                        user = populateFromMap(userConfig);
                    } else {
                        logger.error("user is the wrong type. Only JSON map or YAML map is supported.");
                    }
                } else if(userObject instanceof Map) {
                    Map<String, Object> userConfig = (Map<String, Object>) userObject;
                    user = populateFromMap(userConfig);
                } else {
                    logger.error("user is the wrong type. Only JSON map or YAML map is supported.");
                }
            }
        }
    }

    public List<String> getAddressList() {
        List<String> addressList = new ArrayList<>();
       if (getAddress().getDirectMaps()!=null && !getAddress().getDirectMaps().isEmpty()) {
           getAddress().getDirectMaps().forEach((k,v)->{
               String address = Arrays.asList(k.split(SEPARATE_KEY)).get(0);
               if (!addressList.contains(address)) {
                   addressList.add(address);
               }
           });
        }
        return addressList;
    }

    public List<String> getClientList() {
        List<String> clientList = new ArrayList<>();
        if (getClient().getDirectMaps()!=null && !getClient().getDirectMaps().isEmpty()) {
            getClient().getDirectMaps().forEach((k,v)->{
                String client = Arrays.asList(k.split(SEPARATE_KEY)).get(0);
                if (!clientList.contains(client)) {
                    clientList.add(client);
                }
            });
        }
        return clientList;
    }

    public List<String> getUserList() {
        List<String> userList = new ArrayList<>();
        if (getClient().getDirectMaps()!=null && !getUser().getDirectMaps().isEmpty()) {
            getUser().getDirectMaps().forEach((k,v)->{
                String user = Arrays.asList(k.split(SEPARATE_KEY)).get(0);
                if (!userList.contains(user)) {
                    userList.add(user);
                }
            });
        }
        return userList;
    }

    public static RateLimitSet populateFromMap(Map<String, Object> map) {
        RateLimitSet rateLimitSet = new RateLimitSet();
        map.forEach((k, o)->{
            if (o instanceof String) {
                List<String> limits = Arrays.asList(((String)o).split(" "));
                List<LimitQuota> limitQuota = new ArrayList<>();
                limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
                rateLimitSet.addDirectMap(k, limitQuota);
            } else if (o instanceof Map) {
                Map<String, String> path = (Map<String, String>)o;
                path.forEach((p, v)->{
                    List<String> limits = Arrays.asList(v.split(" "));
                    String key = k + SEPARATE_KEY + p;
                    List<LimitQuota> limitQuotas = new ArrayList<>();
                    limits.stream().forEach(l->limitQuotas.add(new LimitQuota(l)));
                    rateLimitSet.addDirectMap(key, limitQuotas);
                });
            }
        });
        return rateLimitSet;
    }

    public static class RateLimitSet {
        public Map<String, List<LimitQuota>>  directMaps;

        public RateLimitSet() {

        }

        public Map<String, List<LimitQuota>> getDirectMaps() {
            return directMaps;
        }

        public void setDirectMaps(Map<String, List<LimitQuota>> directMaps) {
            this.directMaps = directMaps;
        }

        public void addDirectMap(String key, List<LimitQuota> limitQuotas) {
            if (this.directMaps==null) {
                this.directMaps = new HashMap<>();
            }
            this.directMaps.put(key, limitQuotas);
        }
    }
}
