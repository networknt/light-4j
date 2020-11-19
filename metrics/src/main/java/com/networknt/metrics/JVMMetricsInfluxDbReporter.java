package com.networknt.metrics;

import io.dropwizard.metrics.*;
import io.dropwizard.metrics.influxdb.InfluxDbSender;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class JVMMetricsInfluxDbReporter extends ScheduledReporter {

	private static final Logger logger = LoggerFactory.getLogger(JVMMetricsInfluxDbReporter.class);
	private final InfluxDbSender influxDb;
	private final MetricRegistry registry;
	private final Map<String, String> tags;
	
	public JVMMetricsInfluxDbReporter(final MetricRegistry registry, final InfluxDbSender influxDb, String name, MetricFilter filter, TimeUnit rateUnit,
                                      TimeUnit durationUnit, Map<String, String> tags) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.influxDb = influxDb;
		this.registry = registry;
		this.tags = tags;
	}

	@Override
    public void report(final SortedMap<MetricName, Gauge> gauges, final SortedMap<MetricName, Counter> counters,
                       final SortedMap<MetricName, Histogram> histograms, final SortedMap<MetricName, Meter> meters, final SortedMap<MetricName, Timer> timers) {
        final long now = System.currentTimeMillis();
        
        JVMMetricsUtil.trackAllJVMMetrics(registry, tags);
        
        if(logger.isDebugEnabled()) logger.debug("InfluxDbReporter report is called with counter size " + counters.size());
        try {
            influxDb.flush();
            
            //Get gauges again from registry, since the gauges provided in the argument is OUTDATED (Previous collection)
            for (Map.Entry<MetricName, Gauge> entry : registry.getGauges().entrySet()) {
                reportGauge(entry.getKey(), entry.getValue(), now);
            }
            
            if (influxDb.hasSeriesData()) {
                influxDb.writeData();
            }
        } catch (Exception e) {
            logger.error("Unable to report to InfluxDB. Discarding data.", e);
        }
    }

	private void reportGauge(MetricName name, Gauge<?> gauge, long now) {
        final String value = format(gauge.getValue());
        if(value != null) {
            Map<String, String> apiTags = new HashMap<>(name.getTags());
            String apiName = apiTags.remove("api");
            Map<String, String> clientTags = new HashMap<>(name.getTags());
            String clientId = clientTags.remove("clientId");

            influxDb.appendPoints(new InfluxDbPoint(apiName + "." + name.getKey(), apiTags, now, value));
            if(clientId != null) {
                influxDb.appendPoints(new InfluxDbPoint(clientId + "." + name.getKey(), clientTags, now, value));
            }
        }
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
        return String.format(Locale.US, "%2.4f", v);
    }

}
