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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An {@link ExecutorService} that monitors the number of tasks submitted, running,
 * completed and also keeps a {@link Timer} for the task duration.
 *
 * It will register the metrics using the given (or auto-generated) name as classifier, e.g:
 * "your-executor-service.submitted", "your-executor-service.running", etc.
 */
public class InstrumentedExecutorService implements ExecutorService {
    private static final AtomicLong nameCounter = new AtomicLong();

    private final ExecutorService delegate;
    private final Meter submitted;
    private final Counter running;
    private final Meter completed;
    private final Timer duration;
    private final Meter rejected;

    /**
     * Wraps an {@link ExecutorService} uses an auto-generated default name.
     *
     * @param delegate {@link ExecutorService} to wrap.
     * @param registry {@link MetricRegistry} that will contain the metrics.
     */
    public InstrumentedExecutorService(ExecutorService delegate, MetricRegistry registry) {
        this(delegate, registry, "instrumented-delegate-" + nameCounter.incrementAndGet());
    }

    /**
     * Wraps an {@link ExecutorService} with an explicit name.
     *
     * @param delegate {@link ExecutorService} to wrap.
     * @param registry {@link MetricRegistry} that will contain the metrics.
     * @param name     name for this executor service.
     */
    public InstrumentedExecutorService(ExecutorService delegate, MetricRegistry registry, String name) {
        this.delegate = delegate;
        this.submitted = registry.meter(MetricRegistry.name(name, "submitted"));
        this.running = registry.counter(MetricRegistry.name(name, "running"));
        this.completed = registry.meter(MetricRegistry.name(name, "completed"));
        this.duration = registry.timer(MetricRegistry.name(name, "duration"));
        this.rejected = registry.meter(MetricRegistry.name(name, "rejected"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(@Nonnull Runnable runnable) {
        submitted.mark();
        try {
            delegate.execute(new InstrumentedRunnable(runnable));
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable runnable) {
        submitted.mark();
        try {
            return delegate.submit(new InstrumentedRunnable(runnable));
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable runnable, T result) {
        submitted.mark();
        try {
            return delegate.submit(new InstrumentedRunnable(runnable), result);
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        submitted.mark();
        try {
            return delegate.submit(new InstrumentedCallable<>(task));
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAll(instrumented);
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAll(instrumented, timeout, unit);
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAny(instrumented);
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAny(instrumented, timeout, unit);
        } catch (RejectedExecutionException e) {
            rejected.mark();
            throw e;
        }
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
        instrumented.addAll(tasks.stream().map((Function<Callable<T>, InstrumentedCallable<T>>) InstrumentedCallable::new).collect(Collectors.toList()));
        return instrumented;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, @Nonnull TimeUnit timeUnit) throws InterruptedException {
        return delegate.awaitTermination(l, timeUnit);
    }

    private class InstrumentedRunnable implements Runnable {
        private final Runnable task;

        InstrumentedRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                task.run();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }

    private class InstrumentedCallable<T> implements Callable<T> {
        private final Callable<T> callable;

        InstrumentedCallable(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return callable.call();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }
}
