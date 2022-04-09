package com.networknt.limit;

import com.networknt.limit.key.KeyResolver;
import com.networknt.utility.Constants;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Rate limit logic for light-4j framework. The config will define in the limit.yml config file.
 *
 * By default Rate limit will handle on the server(service) level. But framework support client and address level limitation
 *
 * @author Gavin Chen
 */
public class RateLimiter {

    protected LimitConfig config;

    private Map<String, Map<Long, AtomicLong>> serverTimeMap = new ConcurrentHashMap<>();

    private Map<String, Map<TimeUnit, Map<Long, AtomicLong>>> directTimeMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    static final String ADDRESS_TYPE = "address";
    static final String CLIENT_TYPE = "client";
    static final String USER_TYPE = "user";

    private KeyResolver clientIdKeyResolver;
    private KeyResolver addressKeyResolver;
    private KeyResolver userIdKeyResolver;


    /**
     * Load config and initial model by Rate limit key.
     * @param config LimitConfig object
     * @throws Exception runtime exception
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
                        directTimeMap.put(k, directMap);
                    });
                }
            }
            String addressKey = this.config.getAddressKeyResolver()==null? "com.networknt.limit.key.RemoteAddressKeyResolver":this.config.getAddressKeyResolver();
            addressKeyResolver = (KeyResolver)Class.forName(addressKey).getDeclaredConstructor().newInstance();
        } else if (LimitKey.CLIENT.equals(config.getKey())) {
            if (this.config.getClient()!=null) {
                if (this.config.getClient().getDirectMaps()!=null && !this.config.getClient().getDirectMaps().isEmpty()) {
                    this.config.getClient().getDirectMaps().forEach((k,v)->{
                        Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                        v.forEach(i->{
                            directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                        });
                        directTimeMap.put(k, directMap);
                    });
                }
            }
            String clientIdKey = this.config.getClientIdKeyResolver()==null? "com.networknt.limit.key.JwtClientIdKeyResolver":this.config.getClientIdKeyResolver();
            clientIdKeyResolver = (KeyResolver)Class.forName(clientIdKey).getDeclaredConstructor().newInstance();
        } else if (LimitKey.USER.equals(config.getKey())) {
            if (this.config.getUser()!=null) {
                if (this.config.getUser().getDirectMaps()!=null && !this.config.getUser().getDirectMaps().isEmpty()) {
                    this.config.getUser().getDirectMaps().forEach((k,v)->{
                        Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                        v.forEach(i->{
                            directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                        });
                        directTimeMap.put(k, directMap);
                    });
                }
            }
            String userIdKey = this.config.getUserIdKeyResolver()==null? "com.networknt.limit.key.JwtUserIdKeyResolver":this.config.getUserIdKeyResolver();
            userIdKeyResolver = (KeyResolver)Class.forName(userIdKey).getDeclaredConstructor().newInstance();
        }
    }

    public RateLimitResponse handleRequest(final HttpServerExchange exchange, LimitKey limitKey) {
        if (LimitKey.ADDRESS.equals(limitKey)) {
            String address = addressKeyResolver.resolve(exchange);
            String path = exchange.getRequestPath();
            return isAllowDirect(address, path, ADDRESS_TYPE);
        } else if (LimitKey.CLIENT.equals(limitKey)) {
            String clientId = clientIdKeyResolver.resolve(exchange);
            String path = exchange.getRequestPath();
            return isAllowDirect(clientId, path, CLIENT_TYPE);
        } else if (LimitKey.USER.equals(limitKey)) {
            String userId = userIdKeyResolver.resolve(exchange);
            String path = exchange.getRequestPath();
            return isAllowDirect(userId, path, USER_TYPE);
        } else  {
            //By default, the key is server
            String path = exchange.getRequestPath();
            return isAllowByServer(path);
        }
    }

    /**
     * Handle logic for direct rate limit setting for address, client and user.
     * Use the type for differential the address/client/user
     * @param directKey direct key
     * @param path String
     * @param type String
     * @return RateLimitResponse response
     */
    protected RateLimitResponse isAllowDirect(String directKey, String path, String type) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        Map<TimeUnit, Map<Long, AtomicLong>> localTimeMap;

        String keyWithPath = directKey + LimitConfig.SEPARATE_KEY + path;
        List<LimitQuota> rateLimit;
        String mapKey = directKey;
        if (ADDRESS_TYPE.equalsIgnoreCase(type)) {
            if (config.getAddress() != null && config.getAddress().directMaps.containsKey(keyWithPath)) {
                rateLimit = config.getAddress().directMaps.get(keyWithPath);
                mapKey = keyWithPath;
            } else if (config.getAddress() != null && config.getAddress().directMaps.containsKey(directKey)) {
                rateLimit = config.getAddress().directMaps.get(directKey);
            } else {
                rateLimit = config.rateLimit;
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();

                this.config.getRateLimit().forEach(i->{
                    directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                });
                directTimeMap.put(mapKey, directMap);
            }
        } else if(CLIENT_TYPE.equalsIgnoreCase(type)) {
            if (config.getClient() != null && config.getClient().directMaps.containsKey(keyWithPath)) {
                rateLimit = config.getClient().directMaps.get(keyWithPath);
                mapKey = keyWithPath;
            } else if (config.getClient() != null && config.getClient().directMaps.containsKey(directKey)) {
                rateLimit = config.getClient().directMaps.get(directKey);
            } else {
                rateLimit = config.rateLimit;
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                synchronized(this) {
                    this.config.getRateLimit().forEach(i->{
                        directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                    });
                    directTimeMap.put(mapKey, directMap);
                }
            }
        } else {
            if (config.getUser() != null && config.getUser().directMaps.containsKey(keyWithPath)) {
                rateLimit = config.getUser().directMaps.get(keyWithPath);
                mapKey = keyWithPath;
            } else if (config.getUser() != null && config.getUser().directMaps.containsKey(directKey)) {
                rateLimit = config.getUser().directMaps.get(directKey);
            } else {
                rateLimit = config.rateLimit;
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                synchronized(this) {
                    this.config.getRateLimit().forEach(i->{
                        directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                    });
                    directTimeMap.put(mapKey, directMap);
                }
            }
        }
        localTimeMap = directTimeMap.get(mapKey);
        synchronized(this) {
            for (LimitQuota limitQuota: rateLimit) {
                Map<Long, AtomicLong> timeMap =  localTimeMap.get(limitQuota.getUnit());
                if (timeMap.isEmpty()) {
                    timeMap.put(currentTimeWindow, new AtomicLong(1L));
                    return new RateLimitResponse(true, null);
                } else {
                    Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
                    if (countInOverallTime < limitQuota.value) {
                        //Handle new time windows
                        Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                        timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                        logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                        return new RateLimitResponse(true, null);
                    } else {
                        String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                        return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota, reset));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Handle logic for Server type (key = server) rate limit
     * @param path String
     * @return RateLimitResponse rate limit response
     */
    public RateLimitResponse isAllowByServer(String path) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        if (!serverTimeMap.containsKey(path)) {
            synchronized(this) {
                serverTimeMap.put(path, new ConcurrentHashMap<>());
            }
        }
        LimitQuota limitQuota;
        if (config.getServer() != null && config.getServer().containsKey(path)) {
            limitQuota = this.config.getServer().get(path);
        } else {
            limitQuota = this.config.getRateLimit().get(0);
        }

        Map<Long, AtomicLong> timeMap =  serverTimeMap.get(path);
        synchronized(this) {
            if (timeMap.isEmpty()) {
                timeMap.put(currentTimeWindow, new AtomicLong(1L));
            } else {
                Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
                if (countInOverallTime < limitQuota.value) {
                    //Handle new time windows
                    Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                    timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                    logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                    return new RateLimitResponse(true, null);
                } else {
                    String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                    return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota, reset));
                }
            }
        }
        return new RateLimitResponse(true, null);
    }

    private String getRateLimitReset(Long currentTimeWindow, Map<Long, AtomicLong> timeMap,  LimitQuota limitQuota) {
        String res = null;
        if (TimeUnit.SECONDS.equals(limitQuota.unit)){
            res = "1s";
        } else {
            Optional<Long> firstKey = timeMap.keySet().stream().findFirst();
            long reset = getWindow(limitQuota.unit) + firstKey.get() - currentTimeWindow;
            res = reset + "s";
        }
        return res;
    }

    private Map<String, String> buildHeaders(Long countInOverallTime, LimitQuota limitQuota, String reset) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.RATELIMIT_LIMIT, limitQuota.value + "/" + limitQuota.unit);
        headers.put(Constants.RATELIMIT_REMAINING, String.valueOf(limitQuota.value - countInOverallTime));
        if (reset!=null) {
            headers.put(Constants.RATELIMIT_RESET, reset);
        }

        return headers;
    }

    private  long removeOldEntriesForUser( long currentTimeWindow, Map<Long, AtomicLong> timeWindowVSCountMap, TimeUnit unit)
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
