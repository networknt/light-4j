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

import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.Reservoir;
import io.dropwizard.metrics.Snapshot;
import io.dropwizard.metrics.Timer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.*;

public class TimerTest {
    private final Reservoir reservoir = mock(Reservoir.class);
    private final Clock clock = new Clock() {
        // a mock clock that increments its ticker by 50msec per call
        private long val = 0;

        @Override
        public long getTick() {
            return val += 50000000;
        }
    };
    private final Timer timer = new Timer(reservoir, clock);

    @Test
    public void hasRates() throws Exception {
        assertThat(timer.getCount())
                .isZero();

        assertThat(timer.getMeanRate())
                .isEqualTo(0.0, offset(0.001));

        assertThat(timer.getOneMinuteRate())
                .isEqualTo(0.0, offset(0.001));

        assertThat(timer.getFiveMinuteRate())
                .isEqualTo(0.0, offset(0.001));

        assertThat(timer.getFifteenMinuteRate())
                .isEqualTo(0.0, offset(0.001));
    }

    @Test
    public void updatesTheCountOnUpdates() throws Exception {
        assertThat(timer.getCount())
                .isZero();

        timer.update(1, TimeUnit.SECONDS);

        assertThat(timer.getCount())
                .isEqualTo(1);
    }

    @Test
    public void timesCallableInstances() throws Exception {
        final String value = timer.time(() -> "one");

        assertThat(timer.getCount())
                .isEqualTo(1);

        assertThat(value)
                .isEqualTo("one");

        verify(reservoir).update(50000000);
    }

    @Test
    public void timesRunnableInstances() throws Exception {
        final boolean[] called = {false};
        timer.time((Runnable) () -> called[0] = true);

        assertThat(timer.getCount())
                .isEqualTo(1);

        assertThat(called[0])
                .isTrue();

        verify(reservoir).update(50000000);
    }

    @Test
    public void timesContexts() throws Exception {
        timer.time().stop();

        assertThat(timer.getCount())
                .isEqualTo(1);

        verify(reservoir).update(50000000);
    }

    @Test
    public void returnsTheSnapshotFromTheReservoir() throws Exception {
        final Snapshot snapshot = mock(Snapshot.class);
        when(reservoir.getSnapshot()).thenReturn(snapshot);

        assertThat(timer.getSnapshot())
                .isEqualTo(snapshot);
    }

    @Test
    public void ignoresNegativeValues() throws Exception {
        timer.update(-1, TimeUnit.SECONDS);

        assertThat(timer.getCount())
                .isZero();

        verifyZeroInteractions(reservoir);
    }

    @Test
    public void tryWithResourcesWork() {
        assertThat(timer.getCount()).isZero();

        int dummy = 0;

        try (Timer.Context context = timer.time()) {
            dummy += 1;
        }

        assertThat(timer.getCount())
                .isEqualTo(1);

        verify(reservoir).update(50000000);
    }

}
