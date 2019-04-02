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

import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Reservoir;
import io.dropwizard.metrics.Snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HistogramTest {
    private final Reservoir reservoir = mock(Reservoir.class);
    private final Histogram histogram = new Histogram(reservoir);

    @Test
    public void updatesTheCountOnUpdates() throws Exception {
        assertThat(histogram.getCount())
                .isZero();

        histogram.update(1);

        assertThat(histogram.getCount())
                .isEqualTo(1);
    }

    @Test
    public void returnsTheSnapshotFromTheReservoir() throws Exception {
        final Snapshot snapshot = mock(Snapshot.class);
        when(reservoir.getSnapshot()).thenReturn(snapshot);

        assertThat(histogram.getSnapshot())
                .isEqualTo(snapshot);
    }

    @Test
    public void updatesTheReservoir() throws Exception {
        histogram.update(1);

        verify(reservoir).update(1);
    }
}
