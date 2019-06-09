module com.networknt.metrics {
    exports com.networknt.metrics;
    exports io.dropwizard.metrics;

    requires com.networknt.exception;
    requires com.networknt.status;
    requires com.networknt.client;
    requires com.networknt.mask;
    requires com.networknt.audit;
    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.server;
    requires com.networknt.utility;

    requires java.management;
    requires HdrHistogram;
    requires undertow.core;
    requires org.slf4j;
    requires jdk.unsupported;
    requires jsr305;
}