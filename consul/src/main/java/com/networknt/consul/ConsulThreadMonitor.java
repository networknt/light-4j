package com.networknt.consul;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsulThreadMonitor extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ConsulThreadMonitor.class);
    private final ConcurrentHashMap<String,Long> checkins;

    // MIN_TIME_BETWEEN_CHECKINS_MS accounts for queue-wait time to enter connection pool synchronized methods (for up to 12 queued threads)
    private static final long MIN_TIME_BETWEEN_CHECKINS_MS = 12 * 10 * 1000;

    public ConsulThreadMonitor(final ConcurrentHashMap<String,Long> checkins) {
        this.checkins = checkins;
    }

    public void run() {
        long now;
        while(true) {
            try {
                ConsulConfig config = ConsulConfig.load();
                boolean shutdownIfThreadFrozen = config.isShutdownIfThreadFrozen();
                long waitS = ConsulUtils.getWaitInSecond(config.getWait());
                long timeoutBufferS = ConsulUtils.getTimeoutBufferInSecond(config.getTimeoutBuffer());
                long lookupIntervalS = config.getLookupInterval();
                long maxTimeBetweenCheckinsMs = Math.max(2 * 1000 * ( lookupIntervalS + waitS + timeoutBufferS ), MIN_TIME_BETWEEN_CHECKINS_MS);

                Thread.sleep(maxTimeBetweenCheckinsMs);
                now = System.currentTimeMillis();
                for(Map.Entry<String,Long> checkin : checkins.entrySet()) {
                    if(now - checkin.getValue().longValue() > maxTimeBetweenCheckinsMs) {
                        if(shutdownIfThreadFrozen) {
                            logger.error("Service {} has missed its check in... Shutting down host...", checkin.getKey());
                            ConsulRecoveryManager.gracefulShutdown();
                        } else
                            logger.error("Service {} has missed its check in - Please restart host", checkin.getKey());
                    } else
                        logger.debug("Service {} checked in on time", checkin.getKey());
                }
            } catch (InterruptedException i) { logger.error("Consul Monitor Thread Interrupted", i);
            } catch (Exception e) { logger.error("Consul Monitor Thread Exception", e); }
        }
    }
}
