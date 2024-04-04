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

package io.dropwizard.metrics.influxdb.data;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

/**
 * This class is a bean that holds time series data of a point. A point co relates to a metric.
 */
public class InfluxDbPoint {
    private String measurement;
    private Map<String, String> tags = Collections.emptyMap();
    private long timestamp;
    private String value;

    public InfluxDbPoint(final String measurement, final long timestamp, final String value) {
        this.measurement = measurement;
        this.timestamp = timestamp;
        this.value = value;
    }

    public InfluxDbPoint(final String measurement, final Map<String, String> tags, final long timestamp, final String value) {
        this.measurement = measurement;
        this.timestamp = timestamp;
        this.value = value;
        this.tags = tags;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String t = map2String(tags);
        return measurement +
                (t.length() > 0? "," + t : "") +
                " value=" + value +
                " " + timestamp;
    }

    static public String map2String(final Map<String, String> tags) {
        if(tags != null && !tags.isEmpty()) {
            StringJoiner joined = new StringJoiner(",");
            for (Object o : tags.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                joined.add(pair.getKey() + "=" + pair.getValue());
            }
            return joined.toString();
        } else {
            return "";
        }
    }

}
