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

import org.junit.Test;

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistryListener;
import io.dropwizard.metrics.Timer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MetricRegistryListenerTest {
    private static final MetricName BLAH = MetricName.build("blah");
    
    private final Gauge gauge = mock(Gauge.class);
    private final Counter counter = mock(Counter.class);
    private final Histogram histogram = mock(Histogram.class);
    private final Meter meter = mock(Meter.class);
    private final Timer timer = mock(Timer.class);
    private final MetricRegistryListener listener = new MetricRegistryListener.Base() {

    };

    @Test
    public void noOpsOnGaugeAdded() throws Exception {
        listener.onGaugeAdded(BLAH, gauge);

        verifyZeroInteractions(gauge);
    }

    @Test
    public void noOpsOnCounterAdded() throws Exception {
        listener.onCounterAdded(BLAH, counter);

        verifyZeroInteractions(counter);
    }

    @Test
    public void noOpsOnHistogramAdded() throws Exception {
        listener.onHistogramAdded(BLAH, histogram);

        verifyZeroInteractions(histogram);
    }

    @Test
    public void noOpsOnMeterAdded() throws Exception {
        listener.onMeterAdded(BLAH, meter);

        verifyZeroInteractions(meter);
    }

    @Test
    public void noOpsOnTimerAdded() throws Exception {
        listener.onTimerAdded(BLAH, timer);

        verifyZeroInteractions(timer);
    }

    @Test
    public void doesNotExplodeWhenMetricsAreRemoved() throws Exception {
        listener.onGaugeRemoved(BLAH);
        listener.onCounterRemoved(BLAH);
        listener.onHistogramRemoved(BLAH);
        listener.onMeterRemoved(BLAH);
        listener.onTimerRemoved(BLAH);
    }
}
