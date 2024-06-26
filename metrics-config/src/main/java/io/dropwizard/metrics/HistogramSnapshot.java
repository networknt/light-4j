/*
# Copyfree Open Innovation License

This is version 0.5 of the Copyfree Open Innovation License.

## Terms and Conditions

Redistributions, modified or unmodified, in whole or in part, must retain
applicable copyright or other legal privilege notices, these conditions, and
the following license terms and disclaimer.  Subject to these conditions, the
holder(s) of copyright or other legal privileges, author(s) or assembler(s),
and contributors of this work hereby grant to any person who obtains a copy of
this work in any form:

1. Permission to reproduce, modify, distribute, publish, sell, sublicense, use,
and/or otherwise deal in the licensed material without restriction.

2. A perpetual, worldwide, non-exclusive, royalty-free, irrevocable patent
license to reproduce, modify, distribute, publish, sell, use, and/or otherwise
deal in the licensed material without restriction, for any and all patents:

    a. Held by each such holder of copyright or other legal privilege, author
    or assembler, or contributor, necessarily infringed by the contributions
    alone or by combination with the work, of that privilege holder, author or
    assembler, or contributor.

    b. Necessarily infringed by the work at the time that holder of copyright
    or other privilege, author or assembler, or contributor made any
    contribution to the work.

NO WARRANTY OF ANY KIND IS IMPLIED BY, OR SHOULD BE INFERRED FROM, THIS LICENSE
OR THE ACT OF DISTRIBUTION UNDER THE TERMS OF THIS LICENSE, INCLUDING BUT NOT
LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
AND NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS, ASSEMBLERS, OR HOLDERS OF
COPYRIGHT OR OTHER LEGAL PRIVILEGE BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
LIABILITY, WHETHER IN ACTION OF CONTRACT, TORT, OR OTHERWISE ARISING FROM, OUT
OF, OR IN CONNECTION WITH THE WORK OR THE USE OF OR OTHER DEALINGS IN THE WORK.
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
