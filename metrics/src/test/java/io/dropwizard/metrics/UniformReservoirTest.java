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

import io.dropwizard.metrics.Snapshot;
import io.dropwizard.metrics.UniformReservoir;

import static org.assertj.core.api.Assertions.assertThat;

public class UniformReservoirTest {
    @Test
    @SuppressWarnings("unchecked")
    public void aReservoirOf100OutOf1000Elements() throws Exception {
        final UniformReservoir reservoir = new UniformReservoir(100);
        for (int i = 0; i < 1000; i++) {
            reservoir.update(i);
        }

        final Snapshot snapshot = reservoir.getSnapshot();

        assertThat(reservoir.size())
                .isEqualTo(100);

        assertThat(snapshot.size())
                .isEqualTo(100);

        for (double i : snapshot.getValues()) {
            assertThat(i)
                    .isLessThan(1000)
                    .isGreaterThanOrEqualTo(0);
        }
    }

}
