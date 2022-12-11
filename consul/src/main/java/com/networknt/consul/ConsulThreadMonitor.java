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
    private final ConcurrentHashMap<String,Long> heartbeats;

    private boolean shutdownIfThreadFrozen = config.isShutdownIfThreadFrozen();
    private static final long WAIT_S = ConsulUtils.getWaitInSecond(config.getWait());
    private static final long TIMEOUT_BUFFER_S = ConsulUtils.getTimeoutBufferInSecond(config.getTimeoutBuffer());
    private static final long LOOKUP_INTERVAL_S = config.getLookupInterval();
    private static final long MAX_TIME_BETWEEN_BEATS_MS = 2 * 1000 * ( LOOKUP_INTERVAL_S + WAIT_S + TIMEOUT_BUFFER_S );

    public ConsulThreadMonitor(final ConcurrentHashMap<String,Long> heartbeats) {
        this.heartbeats = heartbeats;
    }

    public void run() {
        long now;
        while(true) {
            try {
                Thread.sleep(MAX_TIME_BETWEEN_BEATS_MS);
                now = System.currentTimeMillis();
                for(Map.Entry<String,Long> beat : heartbeats.entrySet()) {
                    if(now - beat.getValue().longValue() > MAX_TIME_BETWEEN_BEATS_MS) {
                        if(shutdownIfThreadFrozen) {
                            logger.error("Service {} has missed its check in... Restarting host", beat.getKey());
                            ConsulRecoveryManager.gracefulShutdown();
                        } else
                            logger.error("Service {} has missed its check in - Please restart host", beat.getKey());
                    } else
                        logger.debug("Service {} checked in on time", beat.getKey());
                }
            } catch (InterruptedException i) { logger.error("Consul Monitor Thread Interrupted", i);
            } catch (Exception e) { logger.error("Consul Monitor Thread Exception", e); }
        }
    }
}
