package io.dropwizard.metrics.influxdb;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Counting;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.Metered;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.ScheduledReporter;
import io.dropwizard.metrics.Snapshot;
import io.dropwizard.metrics.Timer;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;

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

        public InfluxDbReporter build(final InfluxDbSender influxDb) {
            return new InfluxDbReporter(registry, influxDb, tags, rateUnit, durationUnit, filter, skipIdleMetrics);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(InfluxDbReporter.class);
    private final InfluxDbSender influxDb;
    private final boolean skipIdleMetrics;
    private final Map<MetricName, Long> previousValues;

    private InfluxDbReporter(final MetricRegistry registry, final InfluxDbSender influxDb, final Map<String, String> tags,
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
            // reset counters
            for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
                Counter counter = entry.getValue();
                long count = counter.getCount();
                counter.dec(count);
            }
        } catch (Exception e) {
            logger.error("Unable to report to InfluxDB. Discarding data.", e);
        }
    }

    private void reportTimer(MetricName name, Timer timer, long now) {
        if (canSkipMetric(name, timer)) {
            return;
        }
        final Snapshot snapshot = timer.getSnapshot();

        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("apiName");
        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");

        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".min", apiTags, now, format(convertDuration(snapshot.getMin()))));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".max", apiTags, now, format(convertDuration(snapshot.getMax()))));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".mean", apiTags, now, format(convertDuration(snapshot.getMean()))));

        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".min", clientTags, now, format(convertDuration(snapshot.getMin()))));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".max", clientTags, now, format(convertDuration(snapshot.getMax()))));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".mean", clientTags, now, format(convertDuration(snapshot.getMean()))));
        }
    }

    private void reportHistogram(MetricName name, Histogram histogram, long now) {
        if (canSkipMetric(name, histogram)) {
            return;
        }
        final Snapshot snapshot = histogram.getSnapshot();
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("apiName");
        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");

        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, format(histogram.getCount())));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".min", apiTags, now, format(snapshot.getMin())));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".max", apiTags, now, format(snapshot.getMax())));
        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".mean", apiTags, now, format(snapshot.getMean())));

        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, format(histogram.getCount())));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".min", clientTags, now, format(snapshot.getMin())));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".max", clientTags, now, format(snapshot.getMax())));
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".mean", clientTags, now, format(snapshot.getMean())));
        }
    }

    private void reportCounter(MetricName name, Counter counter, long now) {
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("apiName");
        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");

        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, format(counter.getCount())));
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, format(counter.getCount())));
        }
    }

    private void reportGauge(MetricName name, Gauge<?> gauge, long now) {
        final String value = format(gauge.getValue());
        if(value != null) {
            Map<String, String> apiTags = new HashMap<>(name.getTags());
            String apiName = apiTags.remove("apiName");
            Map<String, String> clientTags = new HashMap<>(name.getTags());
            String clientId = clientTags.remove("clientId");

            influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey(), apiTags, now, value));
            if(clientId != null) {
                influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey(), clientTags, now, value));
            }
        }
    }

    private void reportMeter(MetricName name, Metered meter, long now) {
        if (canSkipMetric(name, meter)) {
            return;
        }
        Map<String, String> apiTags = new HashMap<>(name.getTags());
        String apiName = apiTags.remove("apiName");
        Map<String, String> clientTags = new HashMap<>(name.getTags());
        String clientId = clientTags.remove("clientId");

        influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey() + ".count", apiTags, now, format(meter.getCount())));
        if(clientId != null) {
            influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey() + ".count", clientTags, now, format(meter.getCount())));
        }
    }

    private boolean canSkipMetric(MetricName name, Counting counting) {
        boolean isIdle = (calculateDelta(name, counting.getCount()) == 0);
        if (skipIdleMetrics && !isIdle) {
            previousValues.put(name, counting.getCount());
        }
        return skipIdleMetrics && isIdle;
    }

    private long calculateDelta(MetricName name, long count) {
        Long previous = previousValues.get(name);
        if (previous == null) {
            return -1;
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
