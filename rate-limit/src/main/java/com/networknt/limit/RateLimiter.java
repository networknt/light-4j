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
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    public RateLimiter(LimitConfig config) {
        this.config = config;
        if (LimitKey.SERVER.equals(config.getKey())) {

        } else if (LimitKey.ADDRESS.equals(config.getKey())) {
        } else if (LimitKey.CLIENT.equals(config.getKey())) {
        } else  {
            //Default rate limit
            this.config.getRateLimit().forEach(r->{
                defaultTimeMap.put(r.getUnit(), new HashMap<>());
            });

        }

    }

    public ResponseEntity handleRequest(final HttpServerExchange exchange, LimitKey limitKey) {
        if (LimitKey.SERVER.equals(limitKey)) {
            String path = exchange.getRelativePath();
            if (config.getServer().containsKey(path)) {
                config.getServer().get(path);
            }
        } else if (LimitKey.ADDRESS.equals(limitKey)) {
            InetSocketAddress peer = exchange.getSourceAddress();
            if (config.getAddressList().contains(peer)) {

            }

        } else if (LimitKey.CLIENT.equals(limitKey)) {
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            String clientId = (String)auditInfo.get(Constants.CLIENT_ID_STRING);
            if (config.getClientList().contains(clientId)) {

            }
        } else  {

            //Default rate limit

        }
        return null;
    }

    protected ResponseEntity isAllow(List<LimitQuota> rateLimit) {
        long currentTimeWindow = Instant.now().getEpochSecond();
        for (LimitQuota limitQuota: rateLimit) {
            Map<Long, AtomicLong> timeMap =  defaultTimeMap.get(limitQuota.getUnit());
            if (timeMap.isEmpty()) {
                timeMap.put(currentTimeWindow, new AtomicLong(1L));
            } else {
                Long countInOverallTime = removeOldEntriesForUser(currentTimeWindow, timeMap, limitQuota.unit);
                if (countInOverallTime < limitQuota.value) {
                    //Handle new time windows
                    Long newCount = timeMap.getOrDefault(currentTimeWindow, new AtomicLong(0)).longValue() + 1;
                    timeMap.put(currentTimeWindow, new AtomicLong(newCount));
                    logger.debug("CurrentTimeWindow:" + currentTimeWindow +" Result:true "+ " Count:"+countInOverallTime);
                    //TODO Change below for the detail
                    return null;
                } else {
                    //TODO error response
                    return null;
                }
            }
        }
        return null;
    }

    protected ResponseEntity isAllowByClient(final HttpServerExchange exchange, String clientId) {
        return null;
    }

    protected ResponseEntity isAllowByServer(  LimitQuota limitQuota) {

        return null;
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
