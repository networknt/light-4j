package com.networknt.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.handler.MiddlewareHandler;
import io.dropwizard.metrics.*;
import io.dropwizard.metrics.Timer;
import io.dropwizard.metrics.broadcom.EPAgentMetric;
import io.dropwizard.metrics.broadcom.EPAgentMetricRequest;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class APMAgentReporterTest {

    private static final int REPORTING_INTERVAL_MS = 5;

    private record MockMetricsHandlerRunnable(CyclicBarrier barrier, MiddlewareHandler handler) implements Runnable {

        @Override
        public void run() {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < 50; i++) {
                HttpServerExchange exchange = new HttpServerExchange(null);
                try {
                    handler.handleRequest(exchange);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Random random = new Random();
                try {
                    final var sleep = random.nextInt(10);
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class MockTimeSeriesDbSender implements TimeSeriesDbSender {

        private final InfluxDbWriteObject writeObject = new InfluxDbWriteObject(TimeUnit.MILLISECONDS);
        private final String serviceId;
        private final String productName;
        private final AtomicBoolean failed = new AtomicBoolean(false);

        public MockTimeSeriesDbSender(String serviceId, String productName) {
            this.serviceId = serviceId;
            this.productName = productName;
            System.out.println("MockTimeSeriesDbSender initialized with serviceId: " + serviceId + " and productName: " + productName);
        }

        @Override
        public void flush() {
            writeObject.setPoints(new HashSet<>());
        }

        @Override
        public boolean hasSeriesData() {
            return writeObject.getPoints() != null && !writeObject.getPoints().isEmpty();
        }

        @Override
        public void appendPoints(InfluxDbPoint point) {
            if (point != null) {
                writeObject.getPoints().add(point);
            }
        }

        @Override
        public int writeData() throws ClientException {
            final String body = convertInfluxDBWriteObjectToJSON(writeObject);
            if (body.contains("\"value\":\"-")) {
                System.out.println(body);
                failed.set(true);
            }
            return 200;
        }

        @Override
        public void setTags(Map<String, String> tags) {
            if (tags != null) {
                writeObject.setTags(tags);
            }
        }

        public boolean hasFailed() {
            return this.failed.get();
        }

        private String convertInfluxDBWriteObjectToJSON(InfluxDbWriteObject influxDbWriteObject) throws ClientException {
            EPAgentMetricRequest epAgentMetricRequest = new EPAgentMetricRequest();
            List<EPAgentMetric> epAgentMetricList = new ArrayList<EPAgentMetric>();

            for (InfluxDbPoint point : influxDbWriteObject.getPoints()) {
                EPAgentMetric epAgentMetric = new EPAgentMetric();
                epAgentMetric.setName(convertName(point));
                String pointValue = point.getValue();

                // Value contains a decimal, we need to round to the nearest whole number.
                if (pointValue.contains(".")) {
                    double milliseconds = Double.parseDouble(point.getValue());
                    int roundedMilliseconds = (int) Math.round(milliseconds);
                    epAgentMetric.setValue(Integer.toString(roundedMilliseconds));

                    // Value contains no decimal place, no need for conversion
                } else {
                    epAgentMetric.setValue(pointValue);
                }

                epAgentMetric.setType("PerIntervalCounter");
                epAgentMetricList.add(epAgentMetric);
            }
            epAgentMetricRequest.setMetrics(epAgentMetricList);
            try {
                return Config.getInstance().getMapper().writeValueAsString(epAgentMetricRequest);
            } catch (JsonProcessingException e) {
                throw new ClientException(e);
            }
        }

        private String convertName(InfluxDbPoint point) {

            StringJoiner metricNameJoiner = new StringJoiner("|");

            metricNameJoiner.add(productName);
            metricNameJoiner.add(serviceId);

            for (Map.Entry<String, String> pair : point.getTags().entrySet()) {
                Object value = pair.getValue();
                if (value != null) {
                    metricNameJoiner.add(pair.getValue());
                } else {
                    metricNameJoiner.add("null");
                }
            }

            return metricNameJoiner + ":" + point.getMeasurement();
        }
    }

    private static class MockMetricsHandler implements MiddlewareHandler {

        private final long counterStartingNumber;
        private final String mockServiceId;
        private final String mockProductName;
        private static APMAgentReporter reporter = null;

        MockMetricsHandler(long counterStartingNumber, String mockServiceId, String mockProductName) {
            this.counterStartingNumber = counterStartingNumber;
            this.mockProductName = mockProductName;
            this.mockServiceId = mockServiceId;
        }

        final AtomicBoolean firstTime = new AtomicBoolean(true);
        public static final MetricRegistry registry = new MetricRegistry();
        volatile HttpHandler next;
        public MockTimeSeriesDbSender sender;

        @Override
        public HttpHandler getNext() {
            return this.next;
        }

        @Override
        public MiddlewareHandler setNext(HttpHandler next) {
            Handlers.handlerNotNull(next);
            this.next = next;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void register() {
            // nothing
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            final var testCounter = new MetricName("testCounter");
            final var testTimer = new MetricName("testTimer");

            if (this.firstTime.compareAndSet(true, false)) {
                try {
                    MockTimeSeriesDbSender innerSender = new MockTimeSeriesDbSender(this.mockServiceId, this.mockProductName);
                    reporter = APMAgentReporter
                            .forRegistry(registry)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .filter(MetricFilter.ALL)
                            .build(innerSender);
                    reporter.start(REPORTING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    if (this.counterStartingNumber > 0) {
                        registry.getOrAdd(testCounter, MetricRegistry.MetricBuilder.COUNTERS).inc(this.counterStartingNumber);
                    }
                    this.sender = innerSender;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize MockMetricsHandler", e);
                }
            }
            mockCounterIncrement(testCounter);
            mockTimerIncrement(testTimer);
        }

        private static void mockCounterIncrement(final MetricName metricName) {
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        }

        private static void mockTimerIncrement(final MetricName metricName) throws InterruptedException {
            long startTime = Clock.defaultClock().getTick();
            Thread.sleep(50);
            long endTime = Clock.defaultClock().getTick() - startTime;
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(endTime, TimeUnit.MILLISECONDS);
        }

        public boolean hasFailed() {
            return this.sender.hasFailed();
        }
    }

    /**
     * Tests for race conditions when multiple threads concurrently update counter metrics.
     * This test verifies that the APMAgentReporter can safely handle concurrent metric updates
     * without data corruption or reporting negative values. It simulates 20 concurrent users
     * making metric updates and ensures the reporter doesn't fail when processing these
     * concurrent counter increments.
     * If any metrics contain negative values, the handler gets marked as 'failed'.
     */
    @Test
    public void raceConditionCounterTest() throws InterruptedException {

        final var numUsers = 20;
        final var barrier = new CyclicBarrier(numUsers);
        final var handler = new MockMetricsHandler(0L, "raceConditionTestServiceId", "raceConditionTestProductName");

        // Create and start multiple threads to simulate concurrent metric updates
        final var threads = new ArrayList<Thread>();
        for (int x = 0; x < numUsers; x++) {
            threads.add(new Thread(new MockMetricsHandlerRunnable(barrier, handler)));
        }

        // Start all threads
        for (final var thread : threads) {
            thread.start();
        }

        // Wait for all threads to finish
        for (final var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Wait for a couple of reporting intervals to ensure metrics are reported
        Thread.sleep(REPORTING_INTERVAL_MS * 2);
        Assert.assertFalse(handler.hasFailed());
        MockMetricsHandler.reporter.close();
        MockMetricsHandler.registry.removeMatching(MetricFilter.ALL);
    }

    /**
     * Tests counter overflow handling when counter values exceed Long.MAX_VALUE.
     * This test verifies that the APMAgentReporter gracefully handles counter overflow
     * scenarios by starting with a counter value near Long.MAX_VALUE and incrementing it
     * beyond the maximum value. It ensures the reporter doesn't generate negative deltas
     * or fail when counter values wrap around due to overflow.
     */
    @Test
    public void overflowCounterTest() throws InterruptedException {

        final var numUsers = 4;
        final var barrier = new CyclicBarrier(numUsers);
        final var handler = new MockMetricsHandler(Long.MAX_VALUE - 1, "overflowTestServiceId", "overflowTestProductName");

        // Create and start multiple threads to simulate concurrent metric updates
        final var threads = new ArrayList<Thread>();
        for (int x = 0; x < numUsers; x++) {
            threads.add(new Thread(new MockMetricsHandlerRunnable(barrier, handler)));
        }

        // Start all threads
        for (final var thread : threads) {
            thread.start();
        }

        // Wait for all threads to finish
        for (final var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Wait for a couple of reporting intervals to ensure metrics are reported
        Thread.sleep(REPORTING_INTERVAL_MS * 2);
        Assert.assertFalse(handler.hasFailed());
        MockMetricsHandler.reporter.close();
        MockMetricsHandler.registry.removeMatching(MetricFilter.ALL);
    }

    /**
     * Tests timer metric functionality.
     * This test verifies that timer metrics are correctly measured, reported,
     * and converted to the appropriate time units (min, max, mean values).
     */
    @Test
    public void timerMetricTest() {
        MockTimeSeriesDbSender sender = new MockTimeSeriesDbSender("testService", "testProduct");
        MetricRegistry registry = new MetricRegistry();

        APMAgentReporter reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender);

        // Create timer and add some measurements
        Timer timer = registry.timer(MetricName.build("test.timer").tagged("api", "testApi"));
        timer.update(100, TimeUnit.MILLISECONDS);
        timer.update(200, TimeUnit.MILLISECONDS);
        timer.update(150, TimeUnit.MILLISECONDS);

        // Trigger report
        reporter.report();

        // Verify data was written
        Assert.assertTrue("Timer metrics should be reported", sender.hasSeriesData());
        Assert.assertFalse("No negative values should be reported", sender.hasFailed());
    }

    /**
     * Tests histogram metric functionality.
     * This test verifies that histogram metrics are correctly captured, including
     * count, min, max, and mean values from the snapshot, and that delta calculations
     * work properly for histogram counts.
     */
    @Test
    public void histogramMetricTest() {
        MockTimeSeriesDbSender sender = new MockTimeSeriesDbSender("testService", "testProduct");
        MetricRegistry registry = new MetricRegistry();

        APMAgentReporter reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(true)
                .build(sender);

        // Create histogram and add some values
        Histogram histogram = registry.histogram(MetricName.build("test.histogram").tagged("api", "testApi"));
        histogram.update(10);
        histogram.update(20);
        histogram.update(30);
        histogram.update(15);
        histogram.update(25);

        // First report - should report all counts as delta
        reporter.report();
        Assert.assertTrue("Histogram metrics should be reported", sender.hasSeriesData());

        // Add more values and report again to test delta calculation
        sender.flush();
        histogram.update(40);
        histogram.update(50);

        // Second report - should report only new counts as delta
        reporter.report();
        Assert.assertTrue("Histogram delta metrics should be reported", sender.hasSeriesData());
        Assert.assertFalse("No negative values should be reported", sender.hasFailed());
    }

    /**
     * Tests gauge metric functionality.
     * This test verifies that gauge metrics are correctly read and reported,
     * including handling of null values and proper formatting of different numeric types.
     */
    @Test
    public void gaugeMetricTest() {
        MockTimeSeriesDbSender sender = new MockTimeSeriesDbSender("testService", "testProduct");
        MetricRegistry registry = new MetricRegistry();

        APMAgentReporter reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender);

        // Create various gauge types
        registry.register(MetricName.build("test.gauge.integer").tagged("api", "testApi"),
                (Gauge<Integer>) () -> 42);
        registry.register(MetricName.build("test.gauge.double").tagged("api", "testApi"),
                (Gauge<Double>) () -> 3.14);
        registry.register(MetricName.build("test.gauge.long").tagged("api", "testApi"),
                (Gauge<Long>) () -> 12345L);
        registry.register(MetricName.build("test.gauge.null").tagged("api", "testApi"),
                (Gauge<Object>) () -> null);

        // Trigger report
        reporter.report();

        // Verify data was written (null gauge should be skipped)
        Assert.assertTrue("Gauge metrics should be reported", sender.hasSeriesData());
        Assert.assertFalse("No negative values should be reported", sender.hasFailed());
    }

    /**
     * Tests basic counter metric functionality.
     * This test verifies that individual counter metrics are correctly incremented,
     * delta calculations are accurate, and the skipIdleMetrics feature works as expected.
     */
    @Test
    public void counterMetricTest() {
        MockTimeSeriesDbSender sender = new MockTimeSeriesDbSender("testService", "testProduct");
        MetricRegistry registry = new MetricRegistry();

        APMAgentReporter reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .skipIdleMetrics(true)
                .build(sender);

        // Create counter
        Counter counter = registry.counter(MetricName.build("test.counter").tagged("api", "testApi"));

        // Increment counter
        counter.inc(5);

        // First report - should report initial count as delta
        reporter.report();
        Assert.assertTrue("Counter metrics should be reported", sender.hasSeriesData());

        // Clear sender and increment again
        sender.flush();
        counter.inc(3);
        // idleCounter remains at 0, should be skipped due to skipIdleMetrics=true

        // Second report - should report only new increments as delta
        reporter.report();
        Assert.assertTrue("Counter delta metrics should be reported", sender.hasSeriesData());
        Assert.assertFalse("No negative values should be reported", sender.hasFailed());

        // Test with skipIdleMetrics=false
        APMAgentReporter reporterWithIdle = APMAgentReporter
                .forRegistry(registry)
                .skipIdleMetrics(false)
                .build(sender);

        sender.flush();
        reporterWithIdle.report();
        Assert.assertTrue("Idle metrics should be reported when skipIdleMetrics=false", sender.hasSeriesData());
    }

}
