package com.networknt.consul;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsulThreadMonitor extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ConsulThreadMonitor.class);
    private static final ConsulConfig config =
            (ConsulConfig) Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);
    private final ConcurrentHashMap<String,Long> checkins;

    private boolean shutdownIfThreadFrozen = config.isShutdownIfThreadFrozen();
    private static final long WAIT_S = ConsulUtils.getWaitInSecond(config.getWait());
    private static final long TIMEOUT_BUFFER_S = ConsulUtils.getTimeoutBufferInSecond(config.getTimeoutBuffer());
    private static final long LOOKUP_INTERVAL_S = config.getLookupInterval();
    // MIN_TIME_BETWEEN_CHECKINS_MS accounts for queue-wait time to enter connection pool synchronized methods (for up to 12 queued threads)
    private static final long MIN_TIME_BETWEEN_CHECKINS_MS = 12 * 10 * 1000;
    private static final long MAX_TIME_BETWEEN_CHECKINS_MS = Math.max(2 * 1000 * ( LOOKUP_INTERVAL_S + WAIT_S + TIMEOUT_BUFFER_S ), MIN_TIME_BETWEEN_CHECKINS_MS);

    public ConsulThreadMonitor(final ConcurrentHashMap<String,Long> checkins) {
        this.checkins = checkins;
    }

    public void run() {
        long now;
        while(true) {
            try {
                Thread.sleep(MAX_TIME_BETWEEN_CHECKINS_MS);
                now = System.currentTimeMillis();
                for(Map.Entry<String,Long> checkin : checkins.entrySet()) {
                    if(now - checkin.getValue().longValue() > MAX_TIME_BETWEEN_CHECKINS_MS) {
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
