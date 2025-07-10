package com.networknt.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.broadcom.EPAgentMetric;
import io.dropwizard.metrics.broadcom.EPAgentMetricRequest;
import io.dropwizard.metrics.influxdb.data.InfluxDbPoint;
import io.dropwizard.metrics.influxdb.data.InfluxDbWriteObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

class APMAgentReporterTest {


    private static class MockTimeSeriesDbSender implements TimeSeriesDbSender {

        private final InfluxDbWriteObject writeObject = new InfluxDbWriteObject(TimeUnit.MILLISECONDS);
        private final String serviceId;
        private final String productName;
        private final List<String> report = new ArrayList<>();

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
            this.report.add(body);
            return 200;
        }

        @Override
        public void setTags(Map<String, String> tags) {
            if (tags != null) {
                writeObject.setTags(tags);
            }
        }

        public List<String> getReport() {
            return report;
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

            return metricNameJoiner.toString() + ":" + point.getMeasurement();
        }
    }

    @Test
    void metricsCounterTest() {
        final var registry = new MetricRegistry();
        final var numThreads = 10;
        final var barrier = new CyclicBarrier(numThreads);

        // Create a mock TimeSeriesDbSender to capture the reports
        final var sender = new MockTimeSeriesDbSender("testService", "testProduct");
        final var reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender);
        reporter.start(5, TimeUnit.MILLISECONDS);

        // Create and start multiple threads to simulate concurrent metric updates
        final var threads = new ArrayList<Thread>();
        for (int x = 0; x < numThreads; x++) {
            threads.add(createMockReporterThread(barrier, registry));
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

        // Stop the reporter to flush the metrics
        reporter.stop();

        // Verify that the reports do not contain negative values
        final var reports = sender.getReport();
        for (String report : reports) {
            Assertions.assertFalse(report.contains("\"value\":\"-"));
        }
    }

    private static Thread createMockReporterThread(final CyclicBarrier barrier, final MetricRegistry registry) {
        return new Thread(() -> {
            try {
                // wait for all threads to be ready
                barrier.await();


                Random rand = new Random();
                for (int i = 0; i < 50; i++) {
                    // simulate some work
                    Thread.sleep(rand.nextInt(4));

                    // increment metric on 'work' completion (exchange completion callback)
                    MetricName metricName = new MetricName("testCounter");
                    registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}