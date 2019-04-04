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

import java.util.concurrent.TimeUnit;

public class ManualClock extends Clock {
    long ticksInNanos = 0;

    public synchronized void addNanos(long nanos) {
        ticksInNanos += nanos;
    }

    public synchronized void addSeconds(long seconds) {
        ticksInNanos += TimeUnit.SECONDS.toNanos(seconds);
    }
    
    public synchronized void addMillis(long millis) {
        ticksInNanos += TimeUnit.MILLISECONDS.toNanos(millis);
    }
    
    public synchronized void addHours(long hours) {
        ticksInNanos += TimeUnit.HOURS.toNanos(hours);
    }

    @Override
    public synchronized long getTick() {
        return ticksInNanos;
    }

    @Override
    public synchronized long getTime() {
        return TimeUnit.NANOSECONDS.toMillis(ticksInNanos);
    }
    
}
