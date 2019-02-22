/*
 * Copyright 2010-2013 Coda Hale and Yammer, Inc., 2014-2017 Dropwizard Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

final class HistogramSnapshot extends Snapshot {
    private static final Logger logger = LoggerFactory.getLogger(HistogramSnapshot.class);

    private final Histogram histogram;

    HistogramSnapshot(@Nonnull Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public double getValue(double quantile) {
        return histogram.getValueAtPercentile(quantile * 100.0);
    }

    @Override
    public long[] getValues() {
        long[] vals = new long[(int) histogram.getTotalCount()];
        int i = 0;

        for (HistogramIterationValue value : histogram.recordedValues()) {
            long val = value.getValueIteratedTo();

            for (int j = 0; j < value.getCountAddedInThisIterationStep(); j++) {
                vals[i] = val;

                i++;
            }
        }

        if (i != vals.length) {
            throw new IllegalStateException(
                "Total count was " + histogram.getTotalCount() + " but iterating values produced " + vals.length);
        }

        return vals;
    }

    @Override
    public int size() {
        return (int) histogram.getTotalCount();
    }

    @Override
    public long getMax() {
        return histogram.getMaxValue();
    }

    @Override
    public double getMean() {
        return histogram.getMean();
    }

    @Override
    public long getMin() {
        return histogram.getMinValue();
    }

    @Override
    public double getStdDev() {
        return histogram.getStdDeviation();
    }

    @Override
    public void dump(OutputStream output) {
        PrintWriter p = null;
        try {
            p = new PrintWriter(new OutputStreamWriter(output, UTF_8));
            for (HistogramIterationValue value : histogram.recordedValues()) {
                for (int j = 0; j < value.getCountAddedInThisIterationStep(); j++) {
                    p.printf("%d%n", value.getValueIteratedTo());
                }
            }
        } catch (Exception e) {
            if(p != null) p.close();
            logger.error("Exception:", e);
        }
    }
}
