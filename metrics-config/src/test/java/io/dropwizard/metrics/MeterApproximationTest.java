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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class MeterApproximationTest {

    @ParameterizedTest
    @ValueSource(longs = { 15, 60, 600, 6000 })
    public void controlMeter1MinuteMeanApproximation(long ratePerMinute) throws Exception {
        final Meter meter = simulateMetronome(
                62934, TimeUnit.MILLISECONDS,
                3, TimeUnit.MINUTES, ratePerMinute);

        assertThat(meter.getOneMinuteRate()*60.0)
                .isEqualTo(ratePerMinute, offset(0.1*ratePerMinute));
    }

    @ParameterizedTest
    @ValueSource(longs = { 15, 60, 600, 6000 })
    public void controlMeter5MinuteMeanApproximation(long ratePerMinute) throws Exception {
        final Meter meter = simulateMetronome(
                62934, TimeUnit.MILLISECONDS,
                13, TimeUnit.MINUTES, ratePerMinute);

        assertThat(meter.getFiveMinuteRate()*60.0)
                .isEqualTo(ratePerMinute, offset(0.1*ratePerMinute));
    }

    @ParameterizedTest
    @ValueSource(longs = { 15, 60, 600, 6000 })
    public void controlMeter15MinuteMeanApproximation(long ratePerMinute) throws Exception {
        final Meter meter = simulateMetronome(
                62934, TimeUnit.MILLISECONDS,
                38, TimeUnit.MINUTES, ratePerMinute);

        assertThat(meter.getFifteenMinuteRate()*60.0)
                .isEqualTo(ratePerMinute, offset(0.1*ratePerMinute));
    }

    private Meter simulateMetronome(
            long introDelay, TimeUnit introDelayUnit,
            long duration, TimeUnit durationUnit, 
            long ratePerMinute) {

        final ManualClock clock = new ManualClock();
        final Meter meter = new Meter(clock);

        clock.addNanos(introDelayUnit.toNanos(introDelay));

        final long endTick = clock.getTick() + durationUnit.toNanos(duration);
        final long marksIntervalInNanos = TimeUnit.MINUTES.toNanos(1) / ratePerMinute;

        while (clock.getTick() <= endTick) {
            clock.addNanos(marksIntervalInNanos);
            meter.mark();
        }

        return meter;
    }

}
