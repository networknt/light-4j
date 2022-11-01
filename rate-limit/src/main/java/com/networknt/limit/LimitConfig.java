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
import java.util.concurrent.TimeUnit;

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
    private Config config;


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

    static LimitConfig load() {
        return new LimitConfig();
    }

    static LimitConfig load(String configName) {
        return new LimitConfig(configName);
    }

    void reload() {
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
        } else {
            // set default value to 503.
            errorCode = 503;
        }

        object = getMappedConfig().get(IS_ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
        object = getMappedConfig().get(CLIENT_ID_KEY);
        if(object != null) {
            setClientIdKeyResolver((String) object);
        }
        object = getMappedConfig().get(ADDRESS_KEY);
        if(object != null) {
            setAddressKeyResolver((String) object);
        }
        object = getMappedConfig().get(USER_ID_KEY);
        if(object != null) {
            setUserIdKeyResolver((String) object);
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
        } else {
            // if rateLimit doesn't exist, use the concurrentRequest as request per second.
            List<LimitQuota> limitQuota = new ArrayList<>();
            limitQuota.add(new LimitQuota(concurrentRequest, TimeUnit.SECONDS));
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
                    List<LimitQuota> limitQuotas = new ArrayList<>();
                    limits.stream().forEach(l->limitQuotas.add(new LimitQuota(l)));
                    address.addDirectMap(k, limitQuotas);
                } else if (o instanceof Map) {
                    Map<String, String> path = (Map<String, String>)o;
                    path.forEach((p, v)->{
                        List<String> limits = Arrays.asList(v.split(" "));
                        String key = k + SEPARATE_KEY + p;
                        List<LimitQuota> limitQuotas = new ArrayList<>();
                        limits.stream().forEach(l->limitQuotas.add(new LimitQuota(l)));
                        address.addDirectMap(key, limitQuotas);
                    });
                }
            });

        }

        if (getMappedConfig().get(CLIENT)!=null)  {
            Map<String, Object> client_config = (Map<String, Object>)getMappedConfig().get(CLIENT);
            client = new RateLimitSet();

            client_config.forEach((k, o)->{
                if (o instanceof String) {
                    List<String> limits = Arrays.asList(((String)o).split(" "));
                    List<LimitQuota> limitQuota = new ArrayList<>();
                    limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
                    client.addDirectMap(k, limitQuota);
                } else if (o instanceof Map) {
                    Map<String, String> path = (Map<String, String>)o;
                    path.forEach((p, v)->{
                        List<String> limits = Arrays.asList(v.split(" "));
                        String key = k + SEPARATE_KEY + v;
                        List<LimitQuota> limitQuotas = new ArrayList<>();
                        limits.stream().forEach(l->limitQuotas.add(new LimitQuota(l)));
                        client.addDirectMap(key, limitQuotas);
                    });
                }
            });

        }

        if (getMappedConfig().get(USER)!=null)  {
            Map<String, Object> user_config = (Map<String, Object>)getMappedConfig().get(USER);
            user = new RateLimitSet();

            user_config.forEach((k, o)->{
                if (o instanceof String) {
                    List<String> limits = Arrays.asList(((String)o).split(" "));
                    List<LimitQuota> limitQuota = new ArrayList<>();
                    limits.stream().forEach(l->limitQuota.add(new LimitQuota(l)));
                    user.addDirectMap(k, limitQuota);
                } else if (o instanceof Map) {
                    Map<String, String> path = (Map<String, String>)o;
                    path.forEach((p, v)->{
                        List<String> limits = Arrays.asList(v.split(" "));
                        String key = k + SEPARATE_KEY + v;
                        List<LimitQuota> limitQuotas = new ArrayList<>();
                        limits.stream().forEach(l->limitQuotas.add(new LimitQuota(l)));
                        user.addDirectMap(key, limitQuotas);
                    });
                }
            });

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

    class RateLimitSet{
        Map<String, List<LimitQuota>>  directMaps;

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
