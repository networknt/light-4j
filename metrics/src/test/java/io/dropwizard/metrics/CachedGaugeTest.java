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

import io.dropwizard.metrics.CachedGauge;
import io.dropwizard.metrics.Gauge;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedGaugeTest {
    private final AtomicInteger value = new AtomicInteger(0);
    private final Gauge<Integer> gauge = new CachedGauge<Integer>(100, TimeUnit.MILLISECONDS) {
        @Override
        protected Integer loadValue() {
            return value.incrementAndGet();
        }
    };

    @Test
    public void cachesTheValueForTheGivenPeriod() throws Exception {
        assertThat(gauge.getValue())
                .isEqualTo(1);
        assertThat(gauge.getValue())
                .isEqualTo(1);
    }

    @Test
    public void reloadsTheCachedValueAfterTheGivenPeriod() throws Exception {
        assertThat(gauge.getValue())
                .isEqualTo(1);

        Thread.sleep(150);

        assertThat(gauge.getValue())
                .isEqualTo(2);

        assertThat(gauge.getValue())
                .isEqualTo(2);
    }
}
