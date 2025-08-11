package com.networknt.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.handler.MiddlewareHandler;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
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

    private static class MockTimeSeriesDbSender implements TimeSeriesDbSender {

        private final InfluxDbWriteObject writeObject = new InfluxDbWriteObject(TimeUnit.MILLISECONDS);
        private final String serviceId;
        private final String productName;
        private AtomicBoolean failed = new AtomicBoolean(false);

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
                double milliseconds = Double.parseDouble(point.getValue());
                int roundedMilliseconds = (int) Math.round(milliseconds);
                epAgentMetric.setValue(Integer.toString(roundedMilliseconds));
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
            if (this.firstTime.compareAndSet(true, false)) {
                try {
                    MockTimeSeriesDbSender innerSender = new MockTimeSeriesDbSender("testService", "testProduct");
                    APMAgentReporter reporter = APMAgentReporter
                            .forRegistry(registry)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .filter(MetricFilter.ALL)
                            .build(innerSender);
                    reporter.start(REPORTING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    this.sender = innerSender;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize MockMetricsHandler", e);
                }
            }

            MetricName metricName = new MetricName("testCounter");
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        }

        public boolean hasFailed() {
            return this.sender.hasFailed();
        }
    }

    @Test
    public void raceConditionCounterTest() throws InterruptedException {

        final var numUsers = 20;
        final var barrier = new CyclicBarrier(numUsers);
        final var handler = new MockMetricsHandler();

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
    }

    private record MockMetricsHandlerRunnable(CyclicBarrier barrier, MockMetricsHandler handler) implements Runnable {

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
}
