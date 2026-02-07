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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics for connection pool operations.
 * Thread-safe implementation using atomic counters.
 */
public class SimplePoolMetrics {

    private final Map<URI, UriMetrics> uriMetrics = new ConcurrentHashMap<>();

    /**
     * Get or create metrics for a specific URI.
     * @param uri the URI to get metrics for
     * @return the metrics object for this URI
     */
    public UriMetrics getMetricsForUri(URI uri) {
        return uriMetrics.computeIfAbsent(uri, u -> new UriMetrics());
    }

    /**
     * Get all metrics by URI.
     * @return map of URI to metrics
     */
    public Map<URI, UriMetrics> getAllMetrics() {
        return uriMetrics;
    }

    /**
     * Record a successful connection borrow.
     * @param uri the URI the connection was borrowed for
     */
    public void recordBorrow(URI uri) {
        getMetricsForUri(uri).incrementBorrows();
    }

    /**
     * Record a connection restore.
     * @param uri the URI the connection was restored for
     */
    public void recordRestore(URI uri) {
        getMetricsForUri(uri).incrementRestores();
    }

    /**
     * Record a new connection creation.
     * @param uri the URI the connection was created for
     */
    public void recordConnectionCreated(URI uri) {
        getMetricsForUri(uri).incrementCreated();
    }

    /**
     * Record a connection close.
     * @param uri the URI the connection was closed for
     */
    public void recordConnectionClosed(URI uri) {
        getMetricsForUri(uri).incrementClosed();
    }

    /**
     * Record a borrow failure.
     * @param uri the URI for which borrow failed
     */
    public void recordBorrowFailure(URI uri) {
        getMetricsForUri(uri).incrementBorrowFailures();
    }

    /**
     * Update the current active connection count.
     * @param uri the URI to update
     * @param activeCount the current active connection count
     */
    public void updateActiveConnections(URI uri, int activeCount) {
        getMetricsForUri(uri).setActiveConnections(activeCount);
    }

    /**
     * Generate a metrics summary string for logging.
     * @return formatted metrics summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("SimplePool Metrics:\n");
        for (Map.Entry<URI, UriMetrics> entry : uriMetrics.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Metrics for a single URI.
     */
    public static class UriMetrics {
        private final AtomicLong totalBorrows = new AtomicLong(0);
        private final AtomicLong totalRestores = new AtomicLong(0);
        private final AtomicLong totalCreated = new AtomicLong(0);
        private final AtomicLong totalClosed = new AtomicLong(0);
        private final AtomicLong borrowFailures = new AtomicLong(0);
        private volatile int activeConnections = 0;

        void incrementBorrows() {
            totalBorrows.incrementAndGet();
        }

        void incrementRestores() {
            totalRestores.incrementAndGet();
        }

        void incrementCreated() {
            totalCreated.incrementAndGet();
        }

        void incrementClosed() {
            totalClosed.incrementAndGet();
        }

        void incrementBorrowFailures() {
            borrowFailures.incrementAndGet();
        }

        void setActiveConnections(int count) {
            this.activeConnections = count;
        }

        public long getTotalBorrows() {
            return totalBorrows.get();
        }

        public long getTotalRestores() {
            return totalRestores.get();
        }

        public long getTotalCreated() {
            return totalCreated.get();
        }

        public long getTotalClosed() {
            return totalClosed.get();
        }

        public long getBorrowFailures() {
            return borrowFailures.get();
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        @Override
        public String toString() {
            return String.format(
                "active=%d, borrows=%d, restores=%d, created=%d, closed=%d, failures=%d",
                activeConnections, totalBorrows.get(), totalRestores.get(),
                totalCreated.get(), totalClosed.get(), borrowFailures.get()
            );
        }
    }
}
