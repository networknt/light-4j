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
import org.junit.Test;

import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.JvmAttributeGaugeSet;
import io.dropwizard.metrics.MetricName;

import java.lang.management.RuntimeMXBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JvmAttributeGaugeSetTest {
    private final RuntimeMXBean runtime = mock(RuntimeMXBean.class);
    private final JvmAttributeGaugeSet gauges = new JvmAttributeGaugeSet(runtime);

    @Before
    public void setUp() throws Exception {
        when(runtime.getName()).thenReturn("9928@example.com");

        when(runtime.getVmVendor()).thenReturn("Oracle Corporation");
        when(runtime.getVmName()).thenReturn("Java HotSpot(TM) 64-Bit Server VM");
        when(runtime.getVmVersion()).thenReturn("23.7-b01");
        when(runtime.getSpecVersion()).thenReturn("1.7");
        when(runtime.getUptime()).thenReturn(100L);
    }

    @Test
    public void hasASetOfGauges() throws Exception {
        assertThat(gauges.getMetrics().keySet())
                .containsOnly(
                        MetricName.build("vendor"),
                        MetricName.build("name"),
                        MetricName.build("uptime"));
    }

    @Test
    public void hasAGaugeForTheJVMName() throws Exception {
        final Gauge gauge = (Gauge) gauges.getMetrics().get(MetricName.build("name"));

        assertThat(gauge.getValue())
                .isEqualTo("9928@example.com");
    }

    @Test
    public void hasAGaugeForTheJVMVendor() throws Exception {
        final Gauge gauge = (Gauge) gauges.getMetrics().get(MetricName.build("vendor"));

        assertThat(gauge.getValue())
                .isEqualTo("Oracle Corporation Java HotSpot(TM) 64-Bit Server VM 23.7-b01 (1.7)");
    }

    @Test
    public void hasAGaugeForTheJVMUptime() throws Exception {
        final Gauge gauge = (Gauge) gauges.getMetrics().get(MetricName.build("uptime"));

        assertThat(gauge.getValue())
                .isEqualTo(100L);
    }

    @Test
    public void autoDiscoversTheRuntimeBean() throws Exception {
        final Gauge gauge = (Gauge) new JvmAttributeGaugeSet().getMetrics().get(MetricName.build("uptime"));

        assertThat((Long) gauge.getValue())
                .isPositive();
    }
}
