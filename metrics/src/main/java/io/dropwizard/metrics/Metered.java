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

/**
 * An object which maintains mean and exponentially-weighted rate.
 */
public interface Metered extends Metric, Counting {
    /**
     * Returns the number of events which have been marked.
     *
     * @return the number of events which have been marked
     */
    @Override
    long getCount();

    /**
     * Returns the fifteen-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     *
     * This rate has the same exponential decay factor as the fifteen-minute load average in the
     * {@code top} Unix command.
     *
     * @return the fifteen-minute exponentially-weighted moving average rate at which events have
     *         occurred since the meter was created
     */
    double getFifteenMinuteRate();

    /**
     * Returns the five-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     *
     * This rate has the same exponential decay factor as the five-minute load average in the {@code
     * top} Unix command.
     *
     * @return the five-minute exponentially-weighted moving average rate at which events have
     *         occurred since the meter was created
     */
    double getFiveMinuteRate();

    /**
     * Returns the mean rate at which events have occurred since the meter was created.
     *
     * @return the mean rate at which events have occurred since the meter was created
     */
    double getMeanRate();

    /**
     * Returns the one-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     *
     * This rate has the same exponential decay factor as the one-minute load average in the {@code
     * top} Unix command.
     *
     * @return the one-minute exponentially-weighted moving average rate at which events have
     *         occurred since the meter was created
     */
    double getOneMinuteRate();
}
