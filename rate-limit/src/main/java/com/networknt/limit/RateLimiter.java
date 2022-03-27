package com.networknt.limit;

import com.networknt.limit.key.KeyResolver;
import com.networknt.utility.Constants;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    protected LimitConfig config;
    private Map<TimeUnit, Map<Long, AtomicLong>> defaultTimeMap = new ConcurrentHashMap<>();

    private Map<String, Map<Long, AtomicLong>> serverTimeMap = new ConcurrentHashMap<>();

    private Map<String, Map<TimeUnit, Map<Long, AtomicLong>>> addressDirectTimeMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, Map<Long, AtomicLong>>> addressPathTimeMap = new ConcurrentHashMap<>();

    private Map<String, Map<TimeUnit, Map<Long, AtomicLong>>> clientDirectTimeMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, Map<Long, AtomicLong>>> clientPathTimeMap = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    static final String ADDRESS_TYPE = "address";
    static final String CLIENT_TYPE = "client";

    private KeyResolver clientIdKeyResolver;
    private KeyResolver addressKeyResolver;


    /**
     * Load config and initial model by Rate limit key.
     */
    public RateLimiter(LimitConfig config) throws Exception {
        this.config = config;
        if (LimitKey.SERVER.equals(config.getKey())) {
            if (this.config.getServer()!=null && !this.config.getServer().isEmpty()) {
                this.config.getServer().forEach((k,v)->serverTimeMap.put(k, new ConcurrentHashMap<>()));
            }
        } else if (LimitKey.ADDRESS.equals(config.getKey())) {
            if (this.config.getAddress()!=null) {
                if (this.config.getAddress().getDirectMaps()!=null && !this.config.getAddress().getDirectMaps().isEmpty()) {
                    this.config.getAddress().getDirectMaps().forEach((k,v)->{
                        Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                        v.forEach(i->{
                            directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                        });
                        addressDirectTimeMap.put(k, directMap);
                    });
                }
                if (this.config.getAddress().getPathMaps()!=null && !this.config.getAddress().getPathMaps().isEmpty()) {
                    this.config.getAddress().getPathMaps().forEach((k,v)->{
                        Map<String, Map<Long, AtomicLong>> pathMap = new ConcurrentHashMap<>();
                        v.forEach((p, l)->{
                            pathMap.put(p, new ConcurrentHashMap<>());
                        });
                        addressPathTimeMap.put(k, pathMap);
                    });
                }
            }

        } else if (LimitKey.CLIENT.equals(config.getKey())) {
            if (this.config.getClient()!=null) {
                if (this.config.getClient().getDirectMaps()!=null && !this.config.getClient().getDirectMaps().isEmpty()) {
                    this.config.getClient().getDirectMaps().forEach((k,v)->{
                        Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                        v.forEach(i->{
                            directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                        });
                        clientDirectTimeMap.put(k, directMap);
                    });
                }
                if (this.config.getClient().getPathMaps()!=null && !this.config.getClient().getPathMaps().isEmpty()) {
                    this.config.getClient().getPathMaps().forEach((k,v)->{
                        Map<String, Map<Long, AtomicLong>> pathMap = new ConcurrentHashMap<>();
                        v.forEach((p, l)->{
                            pathMap.put(p, new ConcurrentHashMap<>());
                        });
                        clientPathTimeMap.put(k, pathMap);
                    });

                }
            }
        }
        //Initial Resolvers
        String clientIdKey = this.config.getClientIdKeyResolver()==null? "com.networknt.limit.key.JwtClientIdKeyResolver":this.config.getClientIdKeyResolver();
        String addressKey = this.config.getAddressKeyResolver()==null? "com.networknt.limit.key.RemoteAddressKeyResolver":this.config.getAddressKeyResolver();
        clientIdKeyResolver = (KeyResolver)Class.forName(clientIdKey).getDeclaredConstructor().newInstance();
        addressKeyResolver = (KeyResolver)Class.forName(addressKey).getDeclaredConstructor().newInstance();
    }

    public RateLimitResponse handleRequest(final HttpServerExchange exchange, LimitKey limitKey) {
        if (LimitKey.ADDRESS.equals(limitKey)) {
            String address = addressKeyResolver.resolve(exchange);
            List<LimitQuota> rateLimit = this.config.getAddress().directMaps.get(address);
            if (!config.getAddressList().contains(address)) {
                rateLimit = this.config.rateLimit;
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                this.config.getRateLimit().forEach(i->{
                    directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                });
                addressDirectTimeMap.put(address, directMap);
            }
            if (addressPathTimeMap.containsKey(address)) {
                String path = exchange.getRelativePath();
                return isAllowByPath(address, path, ADDRESS_TYPE);
            } else {
                return isAllowDirect(address, rateLimit, ADDRESS_TYPE);
            }

        } else if (LimitKey.CLIENT.equals(limitKey)) {
            String clientId = clientIdKeyResolver.resolve(exchange);
            List<LimitQuota> rateLimit = this.config.getClient().directMaps.get(clientId);
            if (!config.getClientList().contains(clientId)) {
                rateLimit = this.config.rateLimit;
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                this.config.getRateLimit().forEach(i->{
                    directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                });
                clientDirectTimeMap.put(clientId, directMap);
            }
            if (clientPathTimeMap.containsKey(clientId)) {
                String path = exchange.getRelativePath();
                return isAllowByPath(clientId, path, CLIENT_TYPE);
            } else {
                return isAllowDirect(clientId, rateLimit, CLIENT_TYPE);
            }
        } else  {
            //By default, the key is server
            String path = exchange.getRelativePath();
            LimitQuota quota;
            if (!serverTimeMap.containsKey(path)) {
                serverTimeMap.put(path, new ConcurrentHashMap<>());
                quota = this.config.getRateLimit().get(0);
            } else {
                quota = config.getServer().get(path);
            }
            return isAllowByServer(quota, path);
        }
    }

    /**
     * Handle logic for direct rate limit setting for both address and client
     * use the type for differential the address/client
     */
    protected RateLimitResponse isAllowDirect(String directKey, List<LimitQuota> rateLimit, String type) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        Map<TimeUnit, Map<Long, AtomicLong>> directTimeMap;

        if (ADDRESS_TYPE.equalsIgnoreCase(type)) {
            directTimeMap = addressDirectTimeMap.get(directKey);
        } else {
            directTimeMap = clientDirectTimeMap.get(directKey);
        }
        for (LimitQuota limitQuota: rateLimit) {
            Map<Long, AtomicLong> timeMap =  directTimeMap.get(limitQuota.getUnit());
            if (timeMap.isEmpty()) {
                timeMap.put(currentTimeWindow, new AtomicLong(1L));
                return new RateLimitResponse(true, buildHeaders(1L, limitQuota));
            } else {
                Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
                if (countInOverallTime < limitQuota.value) {
                    //Handle new time windows
                    Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                    timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                    logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                    return new RateLimitResponse(true, buildHeaders(countInOverallTime, limitQuota));
                } else {
                    return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota));
                }
            }
        }
        return null;
    }


    /**
     * Handle logic for path rate limit setting for both address and client
     * use the type for differential the address/client
     */
    protected RateLimitResponse isAllowByPath(String pathKey, String path, String type) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        Map<String, Map<Long, AtomicLong>> pathTimeMap;
        LimitQuota limitQuota;
        if (ADDRESS_TYPE.equalsIgnoreCase(type)) {
            pathTimeMap = addressPathTimeMap.get(pathKey);
            limitQuota = this.config.getAddress().getPathMaps().get(pathKey).getOrDefault(path, this.config.getRateLimit().get(0));
        } else {
            pathTimeMap = clientPathTimeMap.get(pathKey);
            limitQuota = this.config.getClient().getPathMaps().get(pathKey).getOrDefault(path, this.config.getRateLimit().get(0));
        }

        Map<Long, AtomicLong> timeMap =  pathTimeMap.get(path);
        if (timeMap.isEmpty()) {
            timeMap.put(currentTimeWindow, new AtomicLong(1L));
        } else {
            Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
            if (countInOverallTime < limitQuota.value) {
                //Handle new time windows
                Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                return new RateLimitResponse(true, buildHeaders(countInOverallTime, limitQuota));
            } else {
                return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota));
            }
        }
        return new RateLimitResponse(true, buildHeaders(1L, limitQuota));
    }

    /**
     * Handle logic for Server type (key = server) rate limit
     */
    protected RateLimitResponse isAllowByServer(LimitQuota limitQuota, String path) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        Map<Long, AtomicLong> timeMap =  serverTimeMap.get(path);
        if (timeMap.isEmpty()) {
            timeMap.put(currentTimeWindow, new AtomicLong(1L));
        } else {
            Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
            if (countInOverallTime < limitQuota.value) {
                //Handle new time windows
                Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                return new RateLimitResponse(true, buildHeaders(countInOverallTime, limitQuota));
            } else {
                return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota));
            }
        }
        return new RateLimitResponse(true, buildHeaders(1L, limitQuota));
    }

    private Map<String, String> buildHeaders(Long countInOverallTime, LimitQuota limitQuota) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.RATELIMIT_LIMIT, limitQuota.value + "/" + limitQuota.unit);
        headers.put(Constants.RATELIMIT_REMAINING, String.valueOf(limitQuota.value - countInOverallTime));
        return headers;
    }

    private long removeOldEntriesForUser( long currentTimeWindow, Map<Long, AtomicLong> timeWindowVSCountMap, TimeUnit unit)
    {
        List <Long> oldEntriesToBeDeleted=new ArrayList<>();
        long overallCount=0L;
        for (Long timeWindow : timeWindowVSCountMap.keySet()) {
            //Mark old entries (Entries older than the oldest valid time window within the time limit) for deletion
            if ((currentTimeWindow - timeWindow) >= getWindow(unit))
                oldEntriesToBeDeleted.add(timeWindow);
            else
                overallCount+=timeWindowVSCountMap.get(timeWindow).longValue();
        }
        timeWindowVSCountMap.keySet().removeAll(oldEntriesToBeDeleted);
        return overallCount;
    }

    private int getWindow(TimeUnit unit) {
        if (TimeUnit.DAYS.equals(unit)) {
            return 24*60*60;
        } else if (TimeUnit.HOURS.equals(unit)) {
            return 60*60;
        } else if (TimeUnit.MINUTES.equals(unit)) {
            return 60;
        } else {
            return 1;
        }
    }
}
