/*
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
package com.networknt.client.simplepool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Background health checker that periodically validates idle connections
 * and removes stale ones from connection pools.
 */
public class ConnectionHealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHealthChecker.class);

    private final ScheduledExecutorService scheduler;
    private final Supplier<Map<URI, SimpleURIConnectionPool>> poolsSupplier;
    private final long intervalMs;
    private volatile boolean running = false;

    /**
     * Creates a new connection health checker.
     *
     * @param poolsSupplier supplier that provides access to the connection pools map
     * @param intervalMs interval between health checks in milliseconds
     */
    public ConnectionHealthChecker(Supplier<Map<URI, SimpleURIConnectionPool>> poolsSupplier, long intervalMs) {
        this.poolsSupplier = poolsSupplier;
        this.intervalMs = intervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "connection-health-checker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start the health checker background thread.
     */
    public synchronized void start() {
        if (running) {
            logger.debug("ConnectionHealthChecker already running");
            return;
        }
        running = true;
        scheduler.scheduleAtFixedRate(this::checkHealth, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        logger.info("ConnectionHealthChecker started with interval {}ms", intervalMs);
    }

    /**
     * Stop the health checker background thread.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ConnectionHealthChecker stopped");
    }

    /**
     * Performs a health check on all connection pools.
     */
    private void checkHealth() {
        try {
            Map<URI, SimpleURIConnectionPool> pools = poolsSupplier.get();
            if (pools == null || pools.isEmpty()) {
                return;
            }

            int totalCleaned = 0;
            for (Map.Entry<URI, SimpleURIConnectionPool> entry : pools.entrySet()) {
                URI uri = entry.getKey();
                SimpleURIConnectionPool pool = entry.getValue();
                try {
                    int cleaned = pool.validateAndCleanConnections();
                    if (cleaned > 0) {
                        totalCleaned += cleaned;
                        logger.debug("Health check cleaned {} stale connections for {}", cleaned, uri);
                    }
                } catch (Exception e) {
                    logger.warn("Error during health check for {}: {}", uri, e.getMessage());
                }
            }

            if (totalCleaned > 0) {
                logger.info("Connection health check completed: {} stale connections cleaned", totalCleaned);
            } else {
                logger.debug("Connection health check completed: all connections healthy");
            }
        } catch (Exception e) {
            logger.error("Unexpected error during connection health check", e);
        }
    }

    /**
     * Check if the health checker is running.
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
}
