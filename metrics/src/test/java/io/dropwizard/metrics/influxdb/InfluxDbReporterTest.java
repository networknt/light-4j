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

package io.dropwizard.metrics.influxdb;

import static org.mockito.Mockito.*;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.networknt.metrics.TimeSeriesDbSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.Snapshot;
import io.dropwizard.metrics.Timer;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;

public class InfluxDbReporterTest {
    @Mock
    private TimeSeriesDbSender influxDb;
    @Mock
    private InfluxDbWriteObject writeObject;
    @Mock
    private MetricRegistry registry;
    private InfluxDbReporter reporter;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        reporter = InfluxDbReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(influxDb);

    }

    @Test
    public void reportsCounters() throws Exception {
        final Counter counter = mock(Counter.class);
        Mockito.when(counter.getCount()).thenReturn(100L);

        reporter.report(this.map(), this.map("counter", counter), this.map(), this.map(), this.map());
        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();
        System.out.println("point = " + point);
        /*
        assertThat(point.getMeasurement()).isEqualTo("counter");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("count", 100L));
        */
    }

    @Test
    public void reportsHistograms() throws Exception {
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

        reporter.report(this.map(), this.map(), this.map("histogram", histogram), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();


        /*
        assertThat(point.getMeasurement()).isEqualTo("histogram");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(13);
        assertThat(point.getFields()).contains(entry("max", 2L));
        assertThat(point.getFields()).contains(entry("mean", 3.0));
        assertThat(point.getFields()).contains(entry("min", 4L));
        assertThat(point.getFields()).contains(entry("std-dev", 5.0));
        assertThat(point.getFields()).contains(entry("median", 6.0));
        assertThat(point.getFields()).contains(entry("75-percentile", 7.0));
        assertThat(point.getFields()).contains(entry("95-percentile", 8.0));
        assertThat(point.getFields()).contains(entry("98-percentile", 9.0));
        assertThat(point.getFields()).contains(entry("99-percentile", 10.0));
        assertThat(point.getFields()).contains(entry("999-percentile", 11.0));
        */
    }

    @Test
    public void reportsMeters() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        reporter.report(this.map(), this.map(), this.map(), this.map("meter", meter), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("meter");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(5);
        assertThat(point.getFields()).contains(entry("count", 1L));
        assertThat(point.getFields()).contains(entry("one-minute", 2.0));
        assertThat(point.getFields()).contains(entry("five-minute", 3.0));
        assertThat(point.getFields()).contains(entry("fifteen-minute", 4.0));
        assertThat(point.getFields()).contains(entry("mean-rate", 5.0));
        */
    }

    @Test
    public void reportsTimers() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.map(), this.map(), this.map(), this.map(), map("timer", timer));

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("timer");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(17);
        assertThat(point.getFields()).contains(entry("count", 1L));
        assertThat(point.getFields()).contains(entry("mean-rate", 2.0));
        assertThat(point.getFields()).contains(entry("one-minute", 3.0));
        assertThat(point.getFields()).contains(entry("five-minute", 4.0));
        assertThat(point.getFields()).contains(entry("fifteen-minute", 5.0));
        assertThat(point.getFields()).contains(entry("min", 100.0));
        assertThat(point.getFields()).contains(entry("mean", 200.0));
        assertThat(point.getFields()).contains(entry("max", 300.0));
        assertThat(point.getFields()).contains(entry("std-dev", 400.0));
        assertThat(point.getFields()).contains(entry("median", 500.0));
        assertThat(point.getFields()).contains(entry("75-percentile", 600.0));
        assertThat(point.getFields()).contains(entry("95-percentile", 700.0));
        assertThat(point.getFields()).contains(entry("98-percentile", 800.0));
        assertThat(point.getFields()).contains(entry("99-percentile", 900.0));
        assertThat(point.getFields()).contains(entry("999-percentile", 1000.0));
        */
    }

    @Test
    public void reportsIntegerGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1)), this.map(), this.map(), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1));
        */
    }

    @Test
    public void reportsLongGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1L)), this.map(), this.map(), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1L));
        */
    }

    @Test
    public void reportsFloatGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1f)), this.map(), this.map(), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1.1f));
        */
    }

    @Test
    public void reportsDoubleGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1)), this.map(), this.map(), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", 1.1));
        */
    }

    @Test
    public void reportsByteGaugeValues() throws Exception {
        reporter
                .report(map("gauge", gauge((byte) 1)), this.map(), this.map(), this.map(), this.map());

        final ArgumentCaptor<InfluxDbPoint> influxDbPointCaptor = ArgumentCaptor.forClass(InfluxDbPoint.class);
        Mockito.verify(influxDb, atLeastOnce()).appendPoints(influxDbPointCaptor.capture());
        InfluxDbPoint point = influxDbPointCaptor.getValue();

        /*
        assertThat(point.getMeasurement()).isEqualTo("gauge");
        assertThat(point.getFields()).isNotEmpty();
        assertThat(point.getFields()).hasSize(1);
        assertThat(point.getFields()).contains(entry("value", (byte) 1));
        */
    }

    private <T> SortedMap<MetricName, T> map() {
        return new TreeMap<>();
    }

    private <T> SortedMap<MetricName, T> map(String name, T metric) {
        final TreeMap<MetricName, T> map = new TreeMap<>();
        map.put(MetricName.build(name), metric);
        return map;
    }

    private <T> Gauge gauge(T value) {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(value);
        return gauge;
    }
}
