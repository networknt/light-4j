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
