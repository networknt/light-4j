/*
 * Copyright 2010-2013 Coda Hale and Yammer, Inc., 2014-2017 Dropwizard Team
 *
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

package io.dropwizard.metrics.influxdb;

import com.networknt.metrics.TimeSeriesDbSender;
import io.dropwizard.metrics.Timer;
import io.dropwizard.metrics.*;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class InfluxDbReporter extends ScheduledReporter {
    public static final class Builder {
        private final MetricRegistry registry;
        private Map<String, String> tags;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private boolean skipIdleMetrics;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.tags = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Add these tags to all metrics.
         *
         * @param tags a map containing tags common to all metrics
         * @return {@code this}
         */
        public Builder withTags(Map<String, String> tags) {
            this.tags = Collections.unmodifiableMap(tags);
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Only report metrics that have changed.
         *
         * @param skipIdleMetrics true/false for skipping metrics not reported
         * @return {@code this}
         */
        public Builder skipIdleMetrics(boolean skipIdleMetrics) {
            this.skipIdleMetrics = skipIdleMetrics;
            return this;
        }

        public InfluxDbReporter build(final TimeSeriesDbSender influxDb) {
            return new InfluxDbReporter(registry, influxDb, tags, rateUnit, durationUnit, filter, skipIdleMetrics);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(InfluxDbReporter.class);
    private final TimeSeriesDbSender influxDb;
    private final boolean skipIdleMetrics;
    private final Map<MetricName, Long> previousValues;

    private InfluxDbReporter(final MetricRegistry registry, final TimeSeriesDbSender influxDb, final Map<String, String> tags,
                             final TimeUnit rateUnit, final TimeUnit durationUnit, final MetricFilter filter, final boolean skipIdleMetrics) {
        super(registry, "influxDb-reporter", filter, rateUnit, durationUnit);
        this.influxDb = influxDb;
        influxDb.setTags(tags);
        this.skipIdleMetrics = skipIdleMetrics;
        this.previousValues = new TreeMap<>();
    }

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    @Override
    public void report(final SortedMap<MetricName, Gauge> gauges, final SortedMap<MetricName, Counter> counters,
                       final SortedMap<MetricName, Histogram> histograms, final SortedMap<MetricName, Meter> meters, final SortedMap<MetricName, Timer> timers) {
        final long now = System.currentTimeMillis();
        if(logger.isDebugEnabled()) logger.debug("InfluxDbReporter report is called with counter size " + counters.size());
        try {
            influxDb.flush();

            for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
                reportGauge(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
                reportCounter(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
                reportHistogram(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<MetricName, Meter> entry : meters.entrySet()) {
                reportMeter(entry.getKey(), entry.getValue(), now);
            }

            for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
                reportTimer(entry.getKey(), entry.getValue(), now);
            }

            if (influxDb.hasSeriesData()) {
                influxDb.writeData();
            }

            if (logger.isTraceEnabled()) {
                logger.trace("InfluxDbReporter.report() completed successfully at {}", System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.error("Unable to report to InfluxDB. Discarding data.", e);
        }
    }

    private void reportTimer(MetricName name, Timer timer, long now) {
        final Snapshot snapshot = timer.getSnapshot();
        if (logger.isTraceEnabled()) {
            logger.trace("Reporting timer {}: snapshot min={}, max={}, mean={}",
                    name, snapshot.getMin(), snapshot.getMax(), snapshot.getMean());
        }

        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("api");
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".min", apiTags, now, format(convertDuration(snapshot.getMin()))));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".max", apiTags, now, format(convertDuration(snapshot.getMax()))));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".mean", apiTags, now, format(convertDuration(snapshot.getMean()))));

        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".min", clientTags, now, format(convertDuration(snapshot.getMin()))));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".max", clientTags, now, format(convertDuration(snapshot.getMax()))));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".mean", clientTags, now, format(convertDuration(snapshot.getMean()))));
        }
    }

    private void reportHistogram(MetricName name, Histogram histogram, long now) {
        long delta = calculateDelta(name, histogram.getCount());
        this.previousValues.put(name, histogram.getCount());

        if (this.skipIdleMetrics && delta == 0) {
            logger.trace("Skipping histogram {} - zero delta (no activity)", name);
            return;
        }
        final Snapshot snapshot = histogram.getSnapshot();
        if (logger.isTraceEnabled()) {
            logger.trace("Reporting histogram {}: current count={}, delta={}, snapshot min={}, max={}, mean={}",
                    name, histogram.getCount(), delta, snapshot.getMin(), snapshot.getMax(), snapshot.getMean());
        }
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("api");
        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");

        final var formattedDelta = format(delta);
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, formattedDelta));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".min", apiTags, now, format(snapshot.getMin())));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".max", apiTags, now, format(snapshot.getMax())));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".mean", apiTags, now, format(snapshot.getMean())));

        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, formattedDelta));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".min", clientTags, now, format(snapshot.getMin())));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".max", clientTags, now, format(snapshot.getMax())));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".mean", clientTags, now, format(snapshot.getMean())));
        }
    }

    private void reportCounter(MetricName name, Counter counter, long now) {
        long delta = calculateDelta(name, counter.getCount());
        this.previousValues.put(name, counter.getCount());

        if (this.skipIdleMetrics && delta == 0) {
            logger.trace("Skipping counter {} - zero delta (no activity)", name);
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Reporting counter {}: current count={}, delta={}", name, counter.getCount(), delta);
        }

        final var formattedDelta = format(delta);
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("api");
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, formattedDelta));

        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, formattedDelta));
        }
    }

    private void reportGauge(MetricName name, Gauge<?> gauge, long now) {
        final String value = format(gauge.getValue());
        if (value == null) {
            logger.trace("Skipping gauge {} - zero delta (no activity)", name);
            return;
        }

        logger.trace("Reporting gauge {}: value={}", name, value);

        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("api");
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey(), apiTags, now, value));

        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey(), clientTags, now, value));
        }
    }

    private void reportMeter(MetricName name, Metered meter, long now) {
        long delta = calculateDelta(name, meter.getCount());
        this.previousValues.put(name, meter.getCount());

        if (this.skipIdleMetrics && delta == 0) {
            logger.trace("Skipping meter {} - zero delta (no activity)", name);
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Reporting meter {}: current count={}, delta={}", name, meter.getCount(), delta);
        }

        final var formattedDelta = format(delta);
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("api");
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, formattedDelta));

        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, formattedDelta));
        }
    }


    private long calculateDelta(MetricName name, long count) {
        Long previous = previousValues.get(name);
        if (previous == null) {
            logger.debug("First measurement for metric {}: returning count {} as delta", name, count);
            return count;
        }
        if (count < previous) {
            logger.warn("Saw a non-monotonically increasing value for metric '{}'", name);
            return 0;
        }
        return count - previous;
    }

    private String format(Object o) {
        if (o instanceof Float) {
            return format(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return format(((Double) o).doubleValue());
        } else if (o instanceof Byte) {
            return format(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return format(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return format(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return format(((Long) o).longValue());
        }
        return null;
    }
    private String format(long n) {
        return Long.toString(n);
    }

    private String format(double v) {
        // the Carbon plaintext format is pretty underspecified, but it seems like it just wants
        // US-formatted digits
        return String.format(Locale.US, "%2.2f", v);
    }
}
