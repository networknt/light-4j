package com.networknt.consul;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsulRecoveryManager {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRecoveryManager.class);
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);

    private static final AtomicBoolean monitorThreadStarted = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String,Long> heartbeats = new ConcurrentHashMap<>();
    private static final ConsulThreadMonitor consulThreadMonitor = new ConsulThreadMonitor(heartbeats);

    private boolean isRecoveryMode;
    private long recoveryAttempts = 0;
    private String serviceName;
    private String tag;

    public ConsulRecoveryManager(String serviceName, String tag) {
        this.serviceName = serviceName;
        this.tag = tag;
        startConsulThreadMonitor();
    }

    private static void startConsulThreadMonitor() {
        if(monitorThreadStarted.get()) return;
        if(monitorThreadStarted.compareAndSet(false, true)) {
            logger.debug("Starting Consul Thread Monitor...");
            consulThreadMonitor.start();
        }
    }

    /**
     * Exit Consul connection recovery mode
     *
     * @return the previous recovery mode state
     */
    public boolean exitRecoveryMode() {
        recoveryAttempts = 0;
        boolean oldMode = isRecoveryMode;
        isRecoveryMode = false;
        return oldMode;
    }

    /**
     * Record a new failed attempt to recover the Consul connection
     *
     * @return true if additional failed attempts are permitted
     *         false if this new failed attempt was the last permitted attempt
     */
    public boolean newFailedAttempt() {
        isRecoveryMode = true;
        ++recoveryAttempts;
        logger.error("Recovery mode: Fixing Consul Connection for service {} tag {} - attempt {}...", serviceName, tag, recoveryAttempts);
        ConsulConfig config = ConsulConfig.load();
        if(config.getMaxAttemptsBeforeShutdown() == -1) return true;
        return config.getMaxAttemptsBeforeShutdown() >= recoveryAttempts;
    }

    /**
     * Gracefully shuts down the host application
     */
    public static synchronized void gracefulShutdown() {
        if(shutdown.get()) return;
        logger.error("System shutdown initiated - Consul connection could not be reestablished");
        shutdown.set(true);
        System.exit(1);
    }

    public void checkin() {
        logger.debug("Service {} tag {} checking in", serviceName, tag);
        String key = tag == null ? serviceName : serviceName + "|" + tag;
        heartbeats.put(key, System.currentTimeMillis());
    }

    public boolean isRecoveryMode() { return isRecoveryMode; }
    public long getRecoveryAttempts() { return recoveryAttempts; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
