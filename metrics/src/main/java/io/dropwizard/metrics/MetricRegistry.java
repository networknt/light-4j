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

import io.dropwizard.metrics.Counter;
import io.dropwizard.metrics.ExponentiallyDecayingReservoir;
import io.dropwizard.metrics.Gauge;
import io.dropwizard.metrics.Histogram;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.Metric;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.MetricRegistryListener;
import io.dropwizard.metrics.MetricSet;
import io.dropwizard.metrics.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A registry of metric instances.
 */
public class MetricRegistry implements MetricSet {

    /**
     * @param klass klass
     * @param names names
     * @return MetricName
     * @see #name(String, String...)
     */
    public static MetricName name(Class<?> klass, String... names) {
        return name(klass.getName(), names);
    }

    /**
     * Shorthand method for backwards compatibility in creating metric names.
     *
     * Uses {@link MetricName#build(String...)} for its
     * heavy lifting.
     *
     * @see MetricName#build(String...)
     * @param name The first element of the name
     * @param names The remaining elements of the name
     * @return A metric name matching the specified components.
     */
    public static MetricName name(String name, String... names) {
        final int length;

        if (names == null) {
            length = 0;
        } else {
            length = names.length;
        }

        final String[] parts = new String[length + 1];
        parts[0] = name;

        System.arraycopy(names, 0, parts, 1, length);

        return MetricName.build(parts);
    }

    private final ConcurrentMap<MetricName, Metric> metrics;
    private final List<MetricRegistryListener> listeners;

    /**
     * Creates a new {@link MetricRegistry}.
     */
    public MetricRegistry() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Creates a {@link MetricRegistry} with a custom {@link ConcurrentMap} implementation for use
     * inside the registry. Call as the super-constructor to create a {@link MetricRegistry} with
     * space- or time-bounded metric lifecycles, for example.
     * @param metricsMap metricsMap
     */
    protected MetricRegistry(ConcurrentMap<MetricName, Metric> metricsMap) {
        this.metrics = metricsMap;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * @param name name
     * @param metric metric
     * @param <T> type of metrics
     * @return T git s{@code metric}
     * @see #register(MetricName, Metric)
     */
    @SuppressWarnings("unchecked")
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return register(MetricName.build(name), metric);
    }

    /**
     * Given a {@link Metric}, registers it under the given name.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    @SuppressWarnings("unchecked")
    public <T extends Metric> T register(MetricName name, T metric) throws IllegalArgumentException {
        if (metric instanceof MetricSet) {
            registerAll(name, (MetricSet) metric);
        } else {
            final Metric existing = metrics.putIfAbsent(name, metric);
            if (existing == null) {
                onMetricAdded(name, metric);
            } else {
                throw new IllegalArgumentException("A metric named " + name + " already exists");
            }
        }

        return metric;
    }

    /**
     * Given a metric set, registers them.
     *
     * @param metrics    a set of metrics
     * @throws IllegalArgumentException if any of the names are already registered
     */
    public void registerAll(MetricSet metrics) throws IllegalArgumentException {
        registerAll(null, metrics);
    }

    /**
     * @param name name
     * @return Counter counter
     * @see #counter(MetricName)
     */
    public Counter counter(String name) {
        return counter(MetricName.build(name));
    }

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    public Counter counter(MetricName name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    /**
     * @param name name
     * @return Histogram Histogram
     * @see #histogram(MetricName)
     */
    public Histogram histogram(String name) {
        return histogram(MetricName.build(name));
    }

    /**
     * Return the {@link Histogram} registered under this name; or create and register
     * a new {@link Histogram} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Histogram}
     */
    public Histogram histogram(MetricName name) {
        return getOrAdd(name, MetricBuilder.HISTOGRAMS);
    }

    /**
     * @param name name
     * @return Meter Meter
     * @see #meter(MetricName)
     */
    public Meter meter(String name) {
        return meter(MetricName.build(name));
    }

    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Meter}
     */
    public Meter meter(MetricName name) {
        return getOrAdd(name, MetricBuilder.METERS);
    }

    /**
     * @param name name
     * @return Timer Timer
     * @see #timer(MetricName)
     */
    public Timer timer(String name) {
        return timer(MetricName.build(name));
    }

    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Timer}
     */
    public Timer timer(MetricName name) {
        return getOrAdd(name, MetricBuilder.TIMERS);
    }

    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    public boolean remove(MetricName name) {
        final Metric metric = metrics.remove(name);
        if (metric != null) {
            onMetricRemoved(name, metric);
            return true;
        }
        return false;
    }

    /**
     * Removes all the metrics in registry.
     */
    public void removeAll() {
        for(Iterator<Map.Entry<MetricName, Metric>> it = metrics.entrySet().iterator(); it.hasNext();) {
            Map.Entry<MetricName, Metric> entry = it.next();
            Metric metric = entry.getValue();
            if(metric != null) {
                onMetricRemoved(entry.getKey(), metric);
            }
            it.remove();
        }
    }


    /**
     * Removes all metrics which match the given filter.
     *
     * @param filter a filter
     */
    public void removeMatching(MetricFilter filter) {
        metrics.entrySet().stream().filter(entry -> filter.matches(entry.getKey(), entry.getValue())).forEachOrdered(entry -> remove(entry.getKey()));
    }

    /**
     * Adds a {@link MetricRegistryListener} to a collection of listeners that will be notified on
     * metric creation.  Listeners will be notified in the order in which they are added.
     *
     * <b>N.B.:</b> The listener will be notified of all existing metrics when it first registers.
     *
     * @param listener the listener that will be notified
     */
    public void addListener(MetricRegistryListener listener) {
        listeners.add(listener);

        for (Map.Entry<MetricName, Metric> entry : metrics.entrySet()) {
            notifyListenerOfAddedMetric(listener, entry.getValue(), entry.getKey());
        }
    }

    /**
     * Removes a {@link MetricRegistryListener} from this registry's collection of listeners.
     *
     * @param listener the listener that will be removed
     */
    public void removeListener(MetricRegistryListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns a set of the names of all the metrics in the registry.
     *
     * @return the names of all the metrics
     */
    public SortedSet<MetricName> getNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(metrics.keySet()));
    }

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    public SortedMap<MetricName, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the gauges in the registry
     */
    public SortedMap<MetricName, Gauge> getGauges(MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    public SortedMap<MetricName, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the counters in the registry and their names which match the given
     * filter.
     *
     * @param filter    the metric filter to match
     * @return all the counters in the registry
     */
    public SortedMap<MetricName, Counter> getCounters(MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    /**
     * Returns a map of all the histograms in the registry and their names.
     *
     * @return all the histograms in the registry
     */
    public SortedMap<MetricName, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the histograms in the registry and their names which match the given
     * filter.
     *
     * @param filter    the metric filter to match
     * @return all the histograms in the registry
     */
    public SortedMap<MetricName, Histogram> getHistograms(MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    /**
     * Returns a map of all the meters in the registry and their names.
     *
     * @return all the meters in the registry
     */
    public SortedMap<MetricName, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the meters in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the meters in the registry
     */
    public SortedMap<MetricName, Meter> getMeters(MetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    /**
     * Returns a map of all the timers in the registry and their names.
     *
     * @return all the timers in the registry
     */
    public SortedMap<MetricName, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the timers in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the timers in the registry
     */
    public SortedMap<MetricName, Timer> getTimers(MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @SuppressWarnings("unchecked")
    public <T extends Metric> T getOrAdd(MetricName name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return register(name, builder.newMetric());
            } catch (IllegalArgumentException e) {
                final Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricName, T> getMetrics(Class<T> klass, MetricFilter filter) {
        final TreeMap<MetricName, T> timers = new TreeMap<>();
        metrics.entrySet().stream().filter(entry -> klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(),
                entry.getValue())).forEachOrdered(entry -> timers.put(entry.getKey(), (T) entry.getValue()));
        return Collections.unmodifiableSortedMap(timers);
    }

    private void onMetricAdded(MetricName name, Metric metric) {
        for (MetricRegistryListener listener : listeners) {
            notifyListenerOfAddedMetric(listener, metric, name);
        }
    }

    private void notifyListenerOfAddedMetric(MetricRegistryListener listener, Metric metric, MetricName name) {
        if (metric instanceof Gauge) {
            listener.onGaugeAdded(name, (Gauge<?>) metric);
        } else if (metric instanceof Counter) {
            listener.onCounterAdded(name, (Counter) metric);
        } else if (metric instanceof Histogram) {
            listener.onHistogramAdded(name, (Histogram) metric);
        } else if (metric instanceof Meter) {
            listener.onMeterAdded(name, (Meter) metric);
        } else if (metric instanceof Timer) {
            listener.onTimerAdded(name, (Timer) metric);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }

    private void onMetricRemoved(MetricName name, Metric metric) {
        for (MetricRegistryListener listener : listeners) {
            notifyListenerOfRemovedMetric(name, metric, listener);
        }
    }

    private void notifyListenerOfRemovedMetric(MetricName name, Metric metric, MetricRegistryListener listener) {
        if (metric instanceof Gauge) {
            listener.onGaugeRemoved(name);
        } else if (metric instanceof Counter) {
            listener.onCounterRemoved(name);
        } else if (metric instanceof Histogram) {
            listener.onHistogramRemoved(name);
        } else if (metric instanceof Meter) {
            listener.onMeterRemoved(name);
        } else if (metric instanceof Timer) {
            listener.onTimerRemoved(name);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }

    private void registerAll(MetricName prefix, MetricSet metrics) throws IllegalArgumentException {
        if (prefix == null)
            prefix = MetricName.EMPTY;

        for (Map.Entry<MetricName, Metric> entry : metrics.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(MetricName.join(prefix, entry.getKey()), (MetricSet) entry.getValue());
            } else {
                register(MetricName.join(prefix, entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public Map<MetricName, Metric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    /**
     * A quick and easy way of capturing the notion of default metrics.
     */
    public interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric() {
                return new Counter();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric() {
                return new Histogram(new ExponentiallyDecayingReservoir());
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<Meter> METERS = new MetricBuilder<Meter>() {
            @Override
            public Meter newMetric() {
                return new Meter();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Meter.class.isInstance(metric);
            }
        };

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return new Timer();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        T newMetric();

        boolean isInstance(Metric metric);
    }
}
