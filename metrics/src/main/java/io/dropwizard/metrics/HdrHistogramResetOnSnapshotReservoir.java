package io.dropwizard.metrics;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A Reservoir that resets its internal state every time a snapshot is taken. This is useful if you're using snapshots
 * as a means of defining the window in which you want to calculate, say, the 99.9th percentile.
 */
@ThreadSafe
public final class HdrHistogramResetOnSnapshotReservoir implements Reservoir {

    private final Recorder recorder;

    @GuardedBy("this")
    @Nonnull
    private Histogram intervalHistogram;

    /**
     * Create a reservoir with a default recorder. This recorder should be suitable for most usage.
     */
    public HdrHistogramResetOnSnapshotReservoir() {
        this(new Recorder(2));
    }

    /**
     * Create a reservoir with a user-specified recorder.
     *
     * @param recorder Recorder to use
     */
    public HdrHistogramResetOnSnapshotReservoir(Recorder recorder) {
        this.recorder = recorder;

        /*
         * Start by flipping the recorder's interval histogram.
         * - it starts our counting at zero. Arguably this might be a bad thing if you wanted to feed in
         *   a recorder that already had some measurements? But that seems crazy.
         * - intervalHistogram can be nonnull.
         */
        intervalHistogram = recorder.getIntervalHistogram();
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
     * @return the data since the last snapshot was taken
     */
    @Override
    public Snapshot getSnapshot() {
        return new HistogramSnapshot(getDataSinceLastSnapshotAndReset());
    }

    /**
     * @return a copy of the accumulated state since the reservoir last had a snapshot
     */
    @Nonnull
    private synchronized Histogram getDataSinceLastSnapshotAndReset() {
        intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
        return intervalHistogram.copy();
    }
}
