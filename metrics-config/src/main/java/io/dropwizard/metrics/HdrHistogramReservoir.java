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
import org.HdrHistogram.Recorder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class HdrHistogramReservoir implements Reservoir {

    private final Recorder recorder;

    @GuardedBy("this")
    private final Histogram runningTotals;

    @GuardedBy("this")
    @Nonnull
    private Histogram intervalHistogram;

    /**
     * Create a reservoir with a default recorder. This recorder should be suitable for most usage.
     */
    public HdrHistogramReservoir() {
        this(new Recorder(2));
    }

    /**
     * Create a reservoir with a user-specified recorder.
     *
     * @param recorder Recorder to use
     */
    public HdrHistogramReservoir(Recorder recorder) {
        this.recorder = recorder;

        /*
         * Start by flipping the recorder's interval histogram.
         * - it starts our counting at zero. Arguably this might be a bad thing if you wanted to feed in
         *   a recorder that already had some measurements? But that seems crazy.
         * - intervalHistogram can be nonnull.
         * - it lets us figure out the number of significant digits to use in runningTotals.
         */
        intervalHistogram = recorder.getIntervalHistogram();
        runningTotals = new Histogram(intervalHistogram.getNumberOfSignificantValueDigits());
    }

    @Override
    public int size() {
        // This appears to be infrequently called, so not keeping a separate counter just for this.
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        recorder.recordValue(value);
    }

    /**
     * @return the data accumulated since the reservoir was created
     */
    @Override
    public Snapshot getSnapshot() {
        return new HistogramSnapshot(updateRunningTotals());
    }

    /**
     * @return a copy of the accumulated state since the reservoir was created
     */
    @Nonnull
    private synchronized Histogram updateRunningTotals() {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        runningTotals.add(intervalHistogram);
        return runningTotals.copy();
    }
}
