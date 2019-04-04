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

import org.junit.After;
import org.junit.Test;

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.InstrumentedScheduledExecutorService;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.Timer;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

public class InstrumentedScheduledExecutorServiceTest {
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final MetricRegistry registry = new MetricRegistry();
    private final InstrumentedScheduledExecutorService instrumentedScheduledExecutor = new InstrumentedScheduledExecutorService(scheduledExecutor, registry, "xs");

    final Meter submitted = registry.meter("xs.submitted");

    final Counter running = registry.counter("xs.running");
    final Meter completed = registry.meter("xs.completed");
    final Timer duration = registry.timer("xs.duration");

    final Meter scheduledOnce = registry.meter("xs.scheduled.once");
    final Meter scheduledRepetitively = registry.meter("xs.scheduled.repetitively");
    final Counter scheduledOverrun = registry.counter("xs.scheduled.overrun");
    final Histogram percentOfPeriod = registry.histogram("xs.scheduled.percent-of-period");

    @Test
    public void testSubmitRunnable() throws Exception {
        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();

        Future<?> theFuture = instrumentedScheduledExecutor.submit(() -> {
            assertThat(submitted.getCount()).isEqualTo(1);

            assertThat(running.getCount()).isEqualTo(1);
            assertThat(completed.getCount()).isZero();
            assertThat(duration.getCount()).isZero();

            assertThat(scheduledOnce.getCount()).isZero();
            assertThat(scheduledRepetitively.getCount()).isZero();
            assertThat(scheduledOverrun.getCount()).isZero();
            assertThat(percentOfPeriod.getCount()).isZero();
        });

        theFuture.get();

        assertThat(submitted.getCount()).isEqualTo(1);

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isEqualTo(1);
        assertThat(duration.getCount()).isEqualTo(1);
        assertThat(duration.getSnapshot().size()).isEqualTo(1);

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();
    }

    @Test
    public void testScheduleRunnable() throws Exception {
        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();

        ScheduledFuture<?> theFuture = instrumentedScheduledExecutor.schedule(() -> {
            assertThat(submitted.getCount()).isZero();

            assertThat(running.getCount()).isEqualTo(1);
            assertThat(completed.getCount()).isZero();
            assertThat(duration.getCount()).isZero();

            assertThat(scheduledOnce.getCount()).isEqualTo(1);
            assertThat(scheduledRepetitively.getCount()).isZero();
            assertThat(scheduledOverrun.getCount()).isZero();
            assertThat(percentOfPeriod.getCount()).isZero();
        }, 10L, TimeUnit.MILLISECONDS);

        theFuture.get();

        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isEqualTo(1);
        assertThat(duration.getCount()).isEqualTo(1);
        assertThat(duration.getSnapshot().size()).isEqualTo(1);

        assertThat(scheduledOnce.getCount()).isEqualTo(1);
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();
    }

    @Test
    public void testSubmitCallable() throws Exception {
        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();

        final Object obj = new Object();

        Future<Object> theFuture = instrumentedScheduledExecutor.submit(() -> {
            assertThat(submitted.getCount()).isEqualTo(1);

            assertThat(running.getCount()).isEqualTo(1);
            assertThat(completed.getCount()).isZero();
            assertThat(duration.getCount()).isZero();

            assertThat(scheduledOnce.getCount()).isZero();
            assertThat(scheduledRepetitively.getCount()).isZero();
            assertThat(scheduledOverrun.getCount()).isZero();
            assertThat(percentOfPeriod.getCount()).isZero();

            return obj;
        });

        assertThat(theFuture.get()).isEqualTo(obj);

        assertThat(submitted.getCount()).isEqualTo(1);

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isEqualTo(1);
        assertThat(duration.getCount()).isEqualTo(1);
        assertThat(duration.getSnapshot().size()).isEqualTo(1);

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();
    }

    @Test
    public void testScheduleCallable() throws Exception {
        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();

        final Object obj = new Object();

        ScheduledFuture<Object> theFuture = instrumentedScheduledExecutor.schedule(() -> {
            assertThat(submitted.getCount()).isZero();

            assertThat(running.getCount()).isEqualTo(1);
            assertThat(completed.getCount()).isZero();
            assertThat(duration.getCount()).isZero();

            assertThat(scheduledOnce.getCount()).isEqualTo(1);
            assertThat(scheduledRepetitively.getCount()).isZero();
            assertThat(scheduledOverrun.getCount()).isZero();
            assertThat(percentOfPeriod.getCount()).isZero();

            return obj;
        }, 10L, TimeUnit.MILLISECONDS);

        assertThat(theFuture.get()).isEqualTo(obj);

        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isEqualTo(1);
        assertThat(duration.getCount()).isEqualTo(1);
        assertThat(duration.getSnapshot().size()).isEqualTo(1);

        assertThat(scheduledOnce.getCount()).isEqualTo(1);
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();
    }

    @Test
    public void testScheduleFixedRateCallable() throws Exception {
        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isZero();
        assertThat(scheduledOverrun.getCount()).isZero();
        assertThat(percentOfPeriod.getCount()).isZero();

        ScheduledFuture<?> theFuture = instrumentedScheduledExecutor.scheduleAtFixedRate(() -> {
            assertThat(submitted.getCount()).isZero();

            assertThat(running.getCount()).isEqualTo(1);

            assertThat(scheduledOnce.getCount()).isEqualTo(0);
            assertThat(scheduledRepetitively.getCount()).isEqualTo(1);

            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }, 10L, 10L, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(100);
        theFuture.cancel(true);
        TimeUnit.MILLISECONDS.sleep(100);

        assertThat(submitted.getCount()).isZero();

        assertThat(running.getCount()).isZero();
        assertThat(completed.getCount()).isNotEqualTo(0);
        assertThat(duration.getCount()).isNotEqualTo(0);
        assertThat(duration.getSnapshot().size()).isNotEqualTo(0);

        assertThat(scheduledOnce.getCount()).isZero();
        assertThat(scheduledRepetitively.getCount()).isEqualTo(1);
        assertThat(scheduledOverrun.getCount()).isNotEqualTo(0);
        assertThat(percentOfPeriod.getCount()).isNotEqualTo(0);
    }

    @After
    public void tearDown() throws Exception {
        instrumentedScheduledExecutor.shutdown();
        if (!instrumentedScheduledExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
            System.err.println("InstrumentedScheduledExecutorService did not terminate.");
        }
    }

}
