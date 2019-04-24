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

package io.dropwizard.metrics.influxdb;

import java.util.Map;

import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;

public interface InfluxDbSender {
    /**
     * Flushes buffer, if applicable.
     */
    void flush();

    /**
     * @return true if there is data available to send.
     */
    boolean hasSeriesData();

    /**
     * Adds this metric point to the buffer.
     *
     * @param point metric point with tags and fields
     */
    void appendPoints(final InfluxDbPoint point);

    /**
     * Writes buffer data to InfluxDb.
     *
     * @return the response code for the request sent to InfluxDb.
     *
     * @throws Exception exception while writing to InfluxDb api
     */
    int writeData() throws Exception;

    /**
     * Set tags applicable for all the points.
     *
     * @param tags map containing tags common to all metrics
     */
    void setTags(final Map<String, String> tags);
}
