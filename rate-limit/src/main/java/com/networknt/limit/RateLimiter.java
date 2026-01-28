package com.networknt.limit;

import com.networknt.exception.FrameworkException;
import com.networknt.limit.key.KeyResolver;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Rate limit logic for light-4j framework. The config will define in the limit.yml config file.
 *
 * By default, Rate limit will handle on the server(service) level. But framework support client and address level limitation
 *
 * @author Gavin Chen
 */
public class RateLimiter {
    private static final String LIMIT_KEY_NOT_FOUND = "ERR10073";


    private final Map<String, Map<Long, AtomicLong>> serverTimeMap = new ConcurrentHashMap<>();

    private final Map<String, Map<TimeUnit, Map<Long, AtomicLong>>> directTimeMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    static final String UNKNOWN_PREFIX = "/unknown/prefix"; // this is the bucket for all other request path that is not defined in the config
    static final String ADDRESS_TYPE = "address";
    static final String CLIENT_TYPE = "client";
    static final String USER_TYPE = "user";

    private KeyResolver clientIdKeyResolver;
    private KeyResolver addressKeyResolver;
    private KeyResolver userIdKeyResolver;

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

    /**
     * Load config and initial model by Rate limit key.
     * @param config LimitConfig object
     */
    public RateLimiter(LimitConfig config) {
        // Kept for backward compatibility if any, but logic moved to handleRequest
    }

    private void initResolvers(LimitConfig config) throws Exception {
        if (LimitKey.ADDRESS.equals(config.getKey())) {
            String addressKey = config.getAddressKeyResolver()==null? "com.networknt.limit.key.RemoteAddressKeyResolver":config.getAddressKeyResolver();
            if(addressKeyResolver == null || !addressKeyResolver.getClass().getName().equals(addressKey)) {
                addressKeyResolver = (KeyResolver)Class.forName(addressKey).getDeclaredConstructor().newInstance();
            }
        } else if (LimitKey.CLIENT.equals(config.getKey())) {
            String clientIdKey = config.getClientIdKeyResolver()==null? "com.networknt.limit.key.JwtClientIdKeyResolver":config.getClientIdKeyResolver();
            if(clientIdKeyResolver == null || !clientIdKeyResolver.getClass().getName().equals(clientIdKey)) {
                clientIdKeyResolver = (KeyResolver)Class.forName(clientIdKey).getDeclaredConstructor().newInstance();
            }
        } else if (LimitKey.USER.equals(config.getKey())) {
            String userIdKey = config.getUserIdKeyResolver()==null? "com.networknt.limit.key.JwtUserIdKeyResolver":config.getUserIdKeyResolver();
            if(userIdKeyResolver == null || !userIdKeyResolver.getClass().getName().equals(userIdKey)) {
                userIdKeyResolver = (KeyResolver)Class.forName(userIdKey).getDeclaredConstructor().newInstance();
            }
        }
    }

    /**
     * Handle the request with the rate limit logic.
     * @param exchange HttpServerExchange
     * @param config LimitConfig
     * @return RateLimitResponse
     */
    public RateLimitResponse handleRequest(final HttpServerExchange exchange, LimitConfig config) {
        try {
            initResolvers(config);
        } catch (Exception e) {
            logger.error("Exception in initResolvers", e);
            throw new FrameworkException(new Status(LIMIT_KEY_NOT_FOUND, "initResolvers"));
        }
        LimitKey limitKey = config.getKey();
        if (LimitKey.ADDRESS.equals(limitKey)) {
            String address = addressKeyResolver.resolve(exchange);
            if(address == null) {
                logger.error("Failed to resolve the address with the address resolver " + addressKeyResolver.getClass().getPackageName());
                Status status = new Status(LIMIT_KEY_NOT_FOUND, LimitKey.ADDRESS, addressKeyResolver.toString());
                throw new FrameworkException(status);
            }
            String path = exchange.getRequestPath();
            return isAllowDirect(address, path, ADDRESS_TYPE, config);
        } else if (LimitKey.CLIENT.equals(limitKey)) {
            String clientId = clientIdKeyResolver.resolve(exchange);
            if(clientId == null) {
                logger.error("Failed to resolve the clientId with the clientId resolver " + clientIdKeyResolver.getClass().getPackageName()  + ". You must put the limit handler after the security handler in the request/response chain.");
                Status status = new Status(LIMIT_KEY_NOT_FOUND, LimitKey.CLIENT, clientIdKeyResolver.getClass().getPackageName());
                throw new FrameworkException(status);
            }
            String path = exchange.getRequestPath();
            return isAllowDirect(clientId, path, CLIENT_TYPE, config);
        } else if (LimitKey.USER.equals(limitKey)) {
            String userId = userIdKeyResolver.resolve(exchange);
            if(userId == null) {
                logger.error("Failed to resolve the userId with the userId resolver " + userIdKeyResolver.getClass().getPackageName()  + ". You must put the limit handler after the security handler in the request/response chain.");
                Status status = new Status(LIMIT_KEY_NOT_FOUND, LimitKey.USER, userIdKeyResolver.getClass().getPackageName());
                throw new FrameworkException(status);
            }
            String path = exchange.getRequestPath();
            return isAllowDirect(userId, path, USER_TYPE, config);
        } else  {
            //By default, the key is server
            String path = exchange.getRequestPath();
            return isAllowByServer(path, config);
        }
    }

    /**
     * Handle logic for direct rate limit setting for address, client and user.
     * Use the type for differential the address/client/user
     * @param directKey direct key
     * @param path String
     * @param type String
     * @param config LimitConfig
     * @return RateLimitResponse response
     */
    protected RateLimitResponse isAllowDirect(String directKey, String path, String type, LimitConfig config) {
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
                if(logger.isTraceEnabled()) logger.trace("both keyWithPath and directKey not found in the config, use the default rate limit");
                rateLimit = config.getRateLimit();
            }
        } else if(CLIENT_TYPE.equalsIgnoreCase(type)) {
            if (config.getClient() != null && config.getClient().directMaps.containsKey(keyWithPath)) {
                rateLimit = config.getClient().directMaps.get(keyWithPath);
                mapKey = keyWithPath;
            } else if (config.getClient() != null && config.getClient().directMaps.containsKey(directKey)) {
                rateLimit = config.getClient().directMaps.get(directKey);
            } else {
                rateLimit = config.getRateLimit();
            }
        } else {
            if (config.getUser() != null && config.getUser().directMaps.containsKey(keyWithPath)) {
                rateLimit = config.getUser().directMaps.get(keyWithPath);
                mapKey = keyWithPath;
            } else if (config.getUser() != null && config.getUser().directMaps.containsKey(directKey)) {
                rateLimit = config.getUser().directMaps.get(directKey);
            } else {
                rateLimit = config.getRateLimit();
            }
        }

        localTimeMap = directTimeMap.get(mapKey);
        if (localTimeMap == null) {
            synchronized (this) {
                localTimeMap = directTimeMap.get(mapKey);
                if (localTimeMap == null) {
                    Map<TimeUnit, Map<Long, AtomicLong>> map = new ConcurrentHashMap<>();
                    rateLimit.forEach(i -> {
                        map.put(i.getUnit(), new ConcurrentHashMap<>());
                    });
                    directTimeMap.put(mapKey, map);
                    localTimeMap = map;
                }
            }
        }
        synchronized(this) {
            for (LimitQuota limitQuota: rateLimit) {
                Map<Long, AtomicLong> timeMap =  localTimeMap.get(limitQuota.getUnit());
                if (timeMap.isEmpty()) {
                    if(logger.isTraceEnabled()) logger.trace("timeMap is empty, put the first entry");
                    timeMap.put(currentTimeWindow, new AtomicLong(1L));
                    return new RateLimitResponse(true, null);
                } else {
                    Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
                    if(logger.isTraceEnabled()) logger.trace("countInOverallTime: " + countInOverallTime + " limitQuota.value: " + limitQuota.value);
                    if (countInOverallTime < limitQuota.value) {
                        //Handle new time windows
                        Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                        if(logger.isTraceEnabled()) logger.trace("newCount: " + newCount);
                        timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                        logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                        if(config.isHeadersAlwaysSet()) {
                            String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                            return new RateLimitResponse(true, buildHeaders(countInOverallTime, limitQuota, reset, null));
                        } else {
                            return new RateLimitResponse(true, null);
                        }
                    } else {
                        String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                        String retryAfter = getRetryAfter(currentTimeWindow, timeMap, limitQuota);
                        return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota, reset, retryAfter));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Handle logic for Server type (key = server) rate limit
     * @param path String
     * @param config LimitConfig
     * @return RateLimitResponse rate limit response
     */
    public synchronized RateLimitResponse isAllowByServer(String path, LimitConfig config) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        Map<Long, AtomicLong> timeMap = lookupServerTimeMap(path, config);  // defined and unknown one if not defined.
        LimitQuota limitQuota = config.getServer() != null ? lookupLimitQuota(path, config) : null;
        if(limitQuota == null) {
            limitQuota = config.getRateLimit().get(0);
        }
        if (timeMap.isEmpty()) {
            timeMap.put(currentTimeWindow, new AtomicLong(1L));
        } else {
            Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
            if (countInOverallTime < limitQuota.value) {
                //Handle new time windows
                Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                if(config.isHeadersAlwaysSet()) {
                    String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                    return new RateLimitResponse(true, buildHeaders(countInOverallTime, limitQuota, reset, null));
                } else {
                    return new RateLimitResponse(true, null);
                }
            } else {
                String reset = getRateLimitReset(currentTimeWindow, timeMap, limitQuota);
                String retryAfter = getRetryAfter(currentTimeWindow, timeMap, limitQuota);
                return new RateLimitResponse(false, buildHeaders(countInOverallTime, limitQuota, reset, retryAfter));
            }
        }
        return new RateLimitResponse(true, null);
    }

    private Map<Long, AtomicLong> lookupServerTimeMap(String path, LimitConfig config) {
        String prefix = null;
        if (config.getServer() != null) {
            for (String s : config.getServer().keySet()) {
                if (path.startsWith(s)) {
                    prefix = s;
                    break;
                }
            }
        }
        if(prefix == null) {
            // the request path is not in the defined path prefix. Use the default path prefix UNKNOWN_PREFIX.
            if(!serverTimeMap.containsKey(UNKNOWN_PREFIX)) {
                Map<Long, AtomicLong> timeMap = new ConcurrentHashMap<>();
                serverTimeMap.put(UNKNOWN_PREFIX, timeMap);
                return timeMap;
            } else {
                return serverTimeMap.get(UNKNOWN_PREFIX);
            }
        } else {
            return serverTimeMap.computeIfAbsent(prefix, k -> new ConcurrentHashMap<>());
        }

    }

    private LimitQuota lookupLimitQuota(String path, LimitConfig config) {
        String prefix = null;
        for(String s: config.getServer().keySet()) {
            if(path.startsWith(s)) {
                prefix = s;
                break;
            }
        }
        if(prefix == null) {
            return null;
        } else {
            return config.getServer().get(prefix);
        }
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

    /**
     * Get the Retry-After header value
     * @param currentTimeWindow Current time window
     * @param timeMap Time map
     * @param limitQuota Limit quota
     * @return String Retry-After header value
     */
    public String getRetryAfter(Long currentTimeWindow, Map<Long, AtomicLong> timeMap,  LimitQuota limitQuota) {
        String retryAfter = null;
        if (TimeUnit.SECONDS.equals(limitQuota.unit)){
            retryAfter = LocalDateTime.now().plusSeconds(1).format(formatter);
        } else {
            Optional<Long> firstKey = timeMap.keySet().stream().findFirst();
            long reset = getWindow(limitQuota.unit) + firstKey.get() - currentTimeWindow;
            retryAfter = LocalDateTime.now().plusSeconds(reset).format(formatter);
        }
        return retryAfter;
    }

    private Map<String, String> buildHeaders(Long countInOverallTime, LimitQuota limitQuota, String reset, String retryAfter) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.RATELIMIT_LIMIT, limitQuota.value + "/" + limitQuota.unit);
        headers.put(Constants.RATELIMIT_REMAINING, String.valueOf(limitQuota.value - countInOverallTime));
        if (reset!=null) {
            headers.put(Constants.RATELIMIT_RESET, reset);
        }
        if (retryAfter != null) {
            headers.put(Constants.RETRY_AFTER, retryAfter);
        }
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
