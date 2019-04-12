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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@link ScheduledExecutorService} that monitors the number of tasks submitted, running,
 * completed and also keeps a {@link Timer} for the task duration.
 *
 * It will register the metrics using the given (or auto-generated) name as classifier, e.g:
 * "your-executor-service.submitted", "your-executor-service.running", etc.
 */
public class InstrumentedScheduledExecutorService implements ScheduledExecutorService {
    private static final AtomicLong nameCounter = new AtomicLong();

    private final ScheduledExecutorService delegate;

    private final Meter submitted;
    private final Counter running;
    private final Meter completed;
    private final Timer duration;

    private final Meter scheduledOnce;
    private final Meter scheduledRepetitively;
    private final Counter scheduledOverrun;
    private final Histogram percentOfPeriod;

    /**
     * Wraps an {@link ScheduledExecutorService} uses an auto-generated default name.
     *
     * @param delegate {@link ScheduledExecutorService} to wrap.
     * @param registry {@link MetricRegistry} that will contain the metrics.
     */
    public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, MetricRegistry registry) {
        this(delegate, registry, "instrumented-scheduled-executor-service-" + nameCounter.incrementAndGet());
    }

    /**
     * Wraps an {@link ScheduledExecutorService} with an explicit name.
     *
     * @param delegate {@link ScheduledExecutorService} to wrap.
     * @param registry {@link MetricRegistry} that will contain the metrics.
     * @param name     name for this executor service.
     */
    public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, MetricRegistry registry, String name) {
        this.delegate = delegate;

        this.submitted = registry.meter(MetricRegistry.name(name, "submitted"));

        this.running = registry.counter(MetricRegistry.name(name, "running"));
        this.completed = registry.meter(MetricRegistry.name(name, "completed"));
        this.duration = registry.timer(MetricRegistry.name(name, "duration"));

        this.scheduledOnce = registry.meter(MetricRegistry.name(name, "scheduled.once"));
        this.scheduledRepetitively = registry.meter(MetricRegistry.name(name, "scheduled.repetitively"));
        this.scheduledOverrun = registry.counter(MetricRegistry.name(name, "scheduled.overrun"));
        this.percentOfPeriod = registry.histogram(MetricRegistry.name(name, "scheduled.percent-of-period"));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        scheduledOnce.mark();
        return delegate.schedule(new InstrumentedRunnable(command), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        scheduledOnce.mark();
        return delegate.schedule(new InstrumentedCallable<>(callable), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        scheduledRepetitively.mark();
        return delegate.scheduleAtFixedRate(new InstrumentedPeriodicRunnable(command, period, unit), initialDelay, period, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        scheduledRepetitively.mark();
        return delegate.scheduleAtFixedRate(new InstrumentedRunnable(command), initialDelay, delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        submitted.mark();
        return delegate.submit(new InstrumentedCallable<>(task));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        submitted.mark();
        return delegate.submit(new InstrumentedRunnable(task), result);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        submitted.mark();
        return delegate.submit(new InstrumentedRunnable(task));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(new InstrumentedCallable(task));
        }
        return instrumented;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(@Nonnull Runnable command) {
        submitted.mark();
        delegate.execute(new InstrumentedRunnable(command));
    }

    private class InstrumentedRunnable implements Runnable {
        private final Runnable command;

        InstrumentedRunnable(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                command.run();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }

    private class InstrumentedPeriodicRunnable implements Runnable {
        private final Runnable command;
        private final long periodInNanos;

        InstrumentedPeriodicRunnable(Runnable command, long period, TimeUnit unit) {
            this.command = command;
            this.periodInNanos = unit.toNanos(period);
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                command.run();
            } finally {
                final long elapsed = context.stop();
                running.dec();
                completed.mark();
                if (elapsed > periodInNanos) {
                    scheduledOverrun.inc();
                }
                percentOfPeriod.update((100L * elapsed) / periodInNanos);
            }
        }
    }

    private class InstrumentedCallable<T> implements Callable<T> {
        private final Callable<T> task;

        InstrumentedCallable(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return task.call();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }
}
