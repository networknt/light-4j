package com.networknt.limit;

import com.networknt.http.ResponseEntity;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
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

    /**
     * Load config and initial model by Rate limit key.
     */
    public RateLimiter(LimitConfig config) {
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
    }

    public RateLimitResponse handleRequest(final HttpServerExchange exchange, LimitKey limitKey) {
        if (LimitKey.ADDRESS.equals(limitKey)) {
            //TODO change IP address get by resolver
            InetSocketAddress peer = exchange.getSourceAddress();
            String ip = peer.getHostString();

            if (!config.getAddressList().contains(ip)) {
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                this.config.getRateLimit().forEach(i->{
                    directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                });
                addressDirectTimeMap.put(ip, directMap);
            }
            if (addressPathTimeMap.containsKey(ip)) {
                return isAllowByPath(ip, ADDRESS_TYPE);
            } else {
                return isAllowDirect(ip, ADDRESS_TYPE);
            }

        } else if (LimitKey.CLIENT.equals(limitKey)) {
            //TODO change client id get by resolver
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            String clientId = (String)auditInfo.get(Constants.CLIENT_ID_STRING);
            if (!config.getClientList().contains(clientId)) {
                Map<TimeUnit, Map<Long, AtomicLong>> directMap = new ConcurrentHashMap<>();
                this.config.getRateLimit().forEach(i->{
                    directMap.put(i.getUnit(), new ConcurrentHashMap<>());
                });
                clientDirectTimeMap.put(clientId, directMap);
            }
            if (clientPathTimeMap.containsKey(clientId)) {
                return isAllowByPath(clientId, CLIENT_TYPE);
            } else {
                return isAllowDirect(clientId, CLIENT_TYPE);
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
    protected RateLimitResponse isAllowDirect(String directKey, String type) {
        return null;
    }

    /**
     * Handle logic for path rate limit setting for both address and client
     * use the type for differential the address/client
     */
    protected RateLimitResponse isAllowByPath(String pathKey, String type) {
        return null;
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
        //TODO calculate and populate response
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
