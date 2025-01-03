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

package io.dropwizard.metrics;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.ConsoleReporter;
import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.Snapshot;
import io.dropwizard.metrics.Timer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleReporterTest {
    private final MetricRegistry registry = mock(MetricRegistry.class);
    private final Clock clock = mock(Clock.class);
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final PrintStream output = new PrintStream(bytes);
    private final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                                                            .outputTo(output)
                                                            .formattedFor(Locale.US)
                                                            .withClock(clock)
                                                            .formattedFor(TimeZone.getTimeZone("PST"))
                                                            .convertRatesTo(TimeUnit.SECONDS)
                                                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                            .filter(MetricFilter.ALL)
                                                            .build();

    @Before
    public void setUp() throws Exception {
        when(clock.getTime()).thenReturn(1363568676000L);
    }

    @Test
    @Ignore
    public void reportsGaugeValues() throws Exception {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(1);

        reporter.report(map("gauge", gauge),
                        this.map(),
                        this.map(),
                        this.map(),
                        this.map());

        assertThat(consoleOutput())
                .isEqualTo(lines(
                        "3/17/13, 6:04:36 PM ============================================================",
                        "",
                        "-- Gauges ----------------------------------------------------------------------",
                        "gauge",
                        "             value = 1",
                        "",
                        ""
                ));
    }

    @Test
    @Ignore
    public void reportsCounterValues() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        reporter.report(this.map(),
                        map("test.counter", counter),
                        this.map(),
                        this.map(),
                        this.map());

        assertThat(consoleOutput())
                .isEqualTo(lines(
                        "3/17/13, 6:04:36 PM ============================================================",
                        "",
                        "-- Counters --------------------------------------------------------------------",
                        "test.counter",
                        "             count = 100",
                        "",
                        ""
                ));
    }

    @Test
    @Ignore
    public void reportsHistogramValues() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(7.0);
        when(snapshot.get95thPercentile()).thenReturn(8.0);
        when(snapshot.get98thPercentile()).thenReturn(9.0);
        when(snapshot.get99thPercentile()).thenReturn(10.0);
        when(snapshot.get999thPercentile()).thenReturn(11.0);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.map(),
                        this.map(),
                        map("test.histogram", histogram),
                        this.map(),
                        this.map());

        assertThat(consoleOutput())
                .isEqualTo(lines(
                        "3/17/13, 6:04:36 PM ============================================================",
                        "",
                        "-- Histograms ------------------------------------------------------------------",
                        "test.histogram",
                        "             count = 1",
                        "               min = 4",
                        "               max = 2",
                        "              mean = 3.00",
                        "            stddev = 5.00",
                        "            median = 6.00",
                        "              75% <= 7.00",
                        "              95% <= 8.00",
                        "              98% <= 9.00",
                        "              99% <= 10.00",
                        "            99.9% <= 11.00",
                        "",
                        ""
                ));
    }

    @Test
    @Ignore
    public void reportsMeterValues() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2.0);
        when(meter.getOneMinuteRate()).thenReturn(3.0);
        when(meter.getFiveMinuteRate()).thenReturn(4.0);
        when(meter.getFifteenMinuteRate()).thenReturn(5.0);

        reporter.report(this.map(),
                        this.map(),
                        this.map(),
                        map("test.meter", meter),
                        this.map());

        assertThat(consoleOutput())
                .isEqualTo(lines(
                        "3/17/13, 6:04:36 PM ============================================================",
                        "",
                        "-- Meters ----------------------------------------------------------------------",
                        "test.meter",
                        "             count = 1",
                        "         mean rate = 2.00 events/second",
                        "     1-minute rate = 3.00 events/second",
                        "     5-minute rate = 4.00 events/second",
                        "    15-minute rate = 5.00 events/second",
                        "",
                        ""
                ));
    }

    @Test
    @Ignore
    public void reportsTimerValues() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS
                                                                        .toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.map(),
                        this.map(),
                        this.map(),
                        this.map(),
                        map("test.another.timer", timer));

        assertThat(consoleOutput())
                .isEqualTo(lines(
                        "3/17/13, 6:04:36 PM ============================================================",
                        "",
                        "-- Timers ----------------------------------------------------------------------",
                        "test.another.timer",
                        "             count = 1",
                        "         mean rate = 2.00 calls/second",
                        "     1-minute rate = 3.00 calls/second",
                        "     5-minute rate = 4.00 calls/second",
                        "    15-minute rate = 5.00 calls/second",
                        "               min = 300.00 milliseconds",
                        "               max = 100.00 milliseconds",
                        "              mean = 200.00 milliseconds",
                        "            stddev = 400.00 milliseconds",
                        "            median = 500.00 milliseconds",
                        "              75% <= 600.00 milliseconds",
                        "              95% <= 700.00 milliseconds",
                        "              98% <= 800.00 milliseconds",
                        "              99% <= 900.00 milliseconds",
                        "            99.9% <= 1000.00 milliseconds",
                        "",
                        ""
                ));
    }

    private String lines(String... lines) {
        final StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append(String.format("%n"));
        }
        return builder.toString();
    }

    private String consoleOutput() throws UnsupportedEncodingException {
        return bytes.toString("UTF-8");
    }

    private <T> SortedMap<MetricName, T> map() {
        return new TreeMap<>();
    }

    private <T> SortedMap<MetricName, T> map(String name, T metric) {
        final TreeMap<MetricName, T> map = new TreeMap<>();
        map.put(MetricName.build(name), metric);
        return map;
    }
}
